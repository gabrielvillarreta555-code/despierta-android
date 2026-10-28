#!/usr/bin/env bash

set -uo pipefail

diagnostics_dir="${1:?Debe indicarse el directorio de diagnósticos}"
platform_label="${2:?Debe indicarse la versión de Android}"
mkdir -p "$diagnostics_dir"

app_package="com.bluelabstudios.despierta"
test_package="com.bluelabstudios.despierta.test"
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

escape_annotation() {
  sed 's/%/%25/g; s/\r//g' | awk 'BEGIN { ORS = "%0A" } { print }'
}

# Una línea por prueba fallida: Clase#prueba | primera línea de la excepción | causas.
failed_tests() {
  awk '
    function flush_stack() { in_stack = 0 }
    /^INSTRUMENTATION_STATUS: class=/ { cls = substr($0, index($0, "=") + 1); flush_stack(); next }
    /^INSTRUMENTATION_STATUS: test=/  { tst = substr($0, index($0, "=") + 1); flush_stack(); next }
    /^INSTRUMENTATION_STATUS: stack=/ {
      stack = substr($0, index($0, "=") + 1); causes = ""; in_stack = 1; next
    }
    /^INSTRUMENTATION_STATUS_CODE: -2/ {
      sub(/.*\./, "", cls)
      line = cls "#" tst " | " stack
      if (causes != "") line = line " | " causes
      print line
      stack = ""; causes = ""; flush_stack(); next
    }
    /^INSTRUMENTATION_/ { flush_stack(); next }
    in_stack && /Caused by:/ {
      c = $0; sub(/^[ \t]*/, "", c)
      if (length(causes) < 600) causes = causes (causes == "" ? "" : " <- ") c
    }
    in_stack && /^[ \t]*at com\.example\.despierta\./ && !seen_frame[cls tst]++ {
      f = $0; sub(/^[ \t]*/, "", f); stack = stack " [" f "]"
    }
  ' "$1" 2>/dev/null
}

annotation_summary() {
  local output_file="$1"
  local command_status="$2"
  local tests
  tests="$(failed_tests "$output_file")"
  {
    if [[ "$command_status" -eq 124 ]]; then
      echo "La suite superó su tiempo límite y fue detenida."
    fi
    if [[ -n "$tests" ]]; then
      echo "Pruebas fallidas:"
      echo "$tests"
    fi
    grep -E '^(Tests run:|OK \(|INSTRUMENTATION_(RESULT: shortMsg|FAILED|CODE))' "$output_file" 2>/dev/null | tail -4
    if [[ -z "$tests" ]]; then
      echo "Últimas líneas de la instrumentación (código de salida $command_status):"
      tail -15 "$output_file" 2>/dev/null
    fi
  } | cut -c1-900 | head -40 | escape_annotation
}

# Fragmento de logcat de la app y del runner para la suite fallida.
logcat_excerpt() {
  adb logcat -d -v time 2>/dev/null \
    | grep -E 'DespiertaAlarm|TestRunner|AndroidRuntime|FATAL|com\.example\.despierta|ForegroundService|AlarmManager.*despierta|Doze|DeviceIdle' \
    | grep -vE 'TestRunner: (started|finished): ' \
    | tail -45 \
    | cut -c1-300 \
    | escape_annotation
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
  echo "::error title=$suite_label falló en $platform_label::$(annotation_summary "$output_file" "$command_status")"
  echo "::warning title=Logcat de $suite_label ($platform_label)::$(logcat_excerpt)"
  adb logcat -d -v threadtime > "${output_file%.txt}-logcat.txt" 2>&1 || true
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
  900 \
  "$diagnostics_dir/ui-instrumentation.txt"

adb shell settings put system font_scale 1.30
run_suite \
  "Pantalla de alarma" \
  "com.example.despierta.AlarmScreenInstrumentedTest" \
  240 \
  "$diagnostics_dir/alarm-screen-instrumentation.txt"
adb shell settings put system font_scale 1.0 || true

if adb exec-out run-as "$app_package" cat files/Despierta-Alarma-activa.png \
  > "$diagnostics_dir/Despierta-Alarma-activa.png" \
  && [[ -s "$diagnostics_dir/Despierta-Alarma-activa.png" ]]; then
  echo "Captura de alarma activa recuperada"
else
  failures=$((failures + 1))
  echo "::error title=Captura de alarma ($platform_label)::No se pudo recuperar la captura generada por la prueba."
fi

reset_app_state
if adb shell am start -W -n "$app_package/.MainActivity" \
  && sleep 3 \
  && adb exec-out screencap -p > "$diagnostics_dir/Despierta-Inicio.png" \
  && [[ -s "$diagnostics_dir/Despierta-Inicio.png" ]]; then
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
