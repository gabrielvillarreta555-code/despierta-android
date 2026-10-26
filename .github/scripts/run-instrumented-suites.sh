#!/usr/bin/env bash

set -uo pipefail

diagnostics_dir="${1:?Debe indicarse el directorio de diagnósticos}"
platform_label="${2:?Debe indicarse la versión de Android}"
mkdir -p "$diagnostics_dir"

app_package="com.example.despierta"
test_package="com.example.despierta.test"
runner="$test_package/androidx.test.runner.AndroidJUnitRunner"
failures=0

collect_diagnostics() {
  adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
  adb logcat -d -v threadtime > "$diagnostics_dir/logcat.txt" 2>&1 || true
  find "$diagnostics_dir" -maxdepth 1 -name '*-instrumentation.txt' -print0 \
    | sort -z \
    | xargs -0 -r cat > "$diagnostics_dir/combined-instrumentation.txt"
}
trap collect_diagnostics EXIT

annotation_summary() {
  local output_file="$1"
  local summary
  summary="$(
    grep -E 'Error in|FAILURES|Tests run:|AssertionError|Exception|INSTRUMENTATION_(FAILED|CODE)' \
      "$output_file" 2>/dev/null \
      | tail -8 \
      | tr '\n' ' ' \
      | sed 's/%/%25/g; s/\r/%0D/g; s/$/%0A/'
  )"
  if [[ -z "$summary" ]]; then
    summary="La instrumentación no devolvió una línea OK. Consulte el artefacto de diagnósticos.%0A"
  fi
  printf '%s' "$summary"
}

reset_app_state() {
  adb shell am force-stop "$app_package" >/dev/null 2>&1 || true
  adb shell pm clear "$app_package" >/dev/null
  adb shell pm grant "$app_package" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
}

run_suite() {
  local suite_label="$1"
  local classes="$2"
  local timeout_seconds="$3"
  local output_file="$4"

  reset_app_state
  echo "Ejecutando $suite_label en $platform_label"
  timeout "$timeout_seconds" adb shell am instrument -w -r \
    -e class "$classes" \
    "$runner" > "$output_file" 2>&1
  local command_status=$?
  cat "$output_file"

  if [[ $command_status -eq 0 ]] && grep -q '^OK (' "$output_file"; then
    local result
    result="$(grep '^OK (' "$output_file" | tail -1)"
    echo "::notice title=$suite_label ($platform_label)::$result"
    return
  fi

  failures=$((failures + 1))
  echo "::error title=$suite_label falló en $platform_label::$(annotation_summary "$output_file")"
}

adb logcat -c

run_suite \
  "Persistencia y entrega de alarmas" \
  "com.example.despierta.AlarmPersistenceInstrumentedTest,com.example.despierta.AlarmDeliveryInstrumentedTest" \
  480 \
  "$diagnostics_dir/core-instrumentation.txt"

run_suite \
  "Interfaz principal" \
  "com.example.despierta.DespiertaUiInstrumentedTest" \
  300 \
  "$diagnostics_dir/ui-instrumentation.txt"

adb shell settings put system font_scale 1.30
run_suite \
  "Pantalla de alarma" \
  "com.example.despierta.AlarmScreenInstrumentedTest" \
  240 \
  "$diagnostics_dir/alarm-screen-instrumentation.txt"
adb shell settings put system font_scale 1.0 || true

if adb exec-out run-as "$app_package" cat files/Despierta-2.5-Alarma-activa.png \
  > "$diagnostics_dir/Despierta-2.5-Alarma-activa.png" \
  && [[ -s "$diagnostics_dir/Despierta-2.5-Alarma-activa.png" ]]; then
  echo "Captura de alarma activa recuperada"
else
  failures=$((failures + 1))
  echo "::error title=Captura de alarma ($platform_label)::No se pudo recuperar la captura generada por la prueba."
fi

reset_app_state
if adb shell am start -W -n "$app_package/.MainActivity" \
  && sleep 3 \
  && adb exec-out screencap -p > "$diagnostics_dir/Despierta-2.5-Inicio.png" \
  && [[ -s "$diagnostics_dir/Despierta-2.5-Inicio.png" ]]; then
  echo "Captura de inicio recuperada"
else
  failures=$((failures + 1))
  echo "::error title=Captura de inicio ($platform_label)::No se pudo capturar la pantalla principal."
fi

collect_diagnostics

if (( failures > 0 )); then
  echo "$failures comprobaciones instrumentadas fallaron en $platform_label"
  exit 1
fi

echo "Todas las comprobaciones instrumentadas pasaron en $platform_label"
