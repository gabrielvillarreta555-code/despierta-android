#!/usr/bin/env bash
# Ejecuta el recorrido de capturas (ScreenshotTour) y copia las imágenes al directorio indicado.
# La barra de estado se fija con el modo demostración de Android para que las capturas sean
# reproducibles (misma hora, batería y señal; sin notificaciones).

set -uo pipefail

output_dir="${1:?Debe indicarse el directorio de salida}"
mkdir -p "$output_dir"

app_package="com.example.despierta"
runner="com.example.despierta.test/androidx.test.runner.AndroidJUnitRunner"

demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null 2>&1 || true; }

adb shell settings put global sysui_demo_allowed 1 >/dev/null 2>&1 || true
demo enter
demo clock -e hhmm 2142
demo battery -e level 82 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype 4g -e level 4
demo notifications -e visible false
adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true

timeout 900 adb shell am instrument -w -r \
  -e class com.example.despierta.ScreenshotTour \
  "$runner" > "$output_dir/instrumentation.txt" 2>&1
status=$?
demo exit

if [[ $status -ne 0 ]] || grep -q "FAILURES!!!\|INSTRUMENTATION_FAILED\|shortMsg=" "$output_dir/instrumentation.txt"; then
  echo "::warning title=Capturas::El recorrido de capturas terminó con errores; se publican las que se generaron."
fi

count=0
for name in $(adb shell run-as "$app_package" ls files/screenshots 2>/dev/null | tr -d '\r'); do
  [[ "$name" == *.png ]] || continue
  if adb exec-out run-as "$app_package" cat "files/screenshots/$name" > "$output_dir/$name" \
    && [[ -s "$output_dir/$name" ]]; then
    count=$((count + 1))
  else
    rm -f "$output_dir/$name"
  fi
done
adb logcat -d -s DespiertaCapturas:V > "$output_dir/tour-log.txt" 2>&1 || true

echo "Capturas copiadas: $count"
if [[ $count -eq 0 ]]; then
  echo "::error title=Capturas::No se generó ninguna captura."
  tail -n 40 "$output_dir/instrumentation.txt" || true
  exit 1
fi
