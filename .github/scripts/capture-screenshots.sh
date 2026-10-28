#!/usr/bin/env bash
# Ejecuta el recorrido de capturas (ScreenshotTour) y copia las imágenes al directorio indicado.
# La barra de estado se fija con el modo demostración de Android para que las capturas sean
# reproducibles (misma hora, batería y señal; sin notificaciones).

set -uo pipefail

output_dir="${1:?Debe indicarse el directorio de salida}"
mkdir -p "$output_dir"

app_package="com.bluelabstudios.despierta"
runner="com.bluelabstudios.despierta.test/androidx.test.runner.AndroidJUnitRunner"

demo() {
  adb shell am broadcast -a com.android.systemui.demo -e command "$@" >> "$output_dir/demo-mode.txt" 2>&1 || true
}

# Preparar el emulador: sin diálogos que tapen la app.
#  - Permiso de notificaciones concedido (si no, la app muestra el diálogo al abrirse).
#  - Diálogos de error del sistema ocultos ("Pixel Launcher no responde" en emuladores lentos).
adb shell pm grant "$app_package" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
adb shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
# Deja que el lanzador termine de arrancar antes de empezar.
sleep 25
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true

adb shell settings put global sysui_demo_allowed 1 >/dev/null 2>&1 || true
sleep 2
demo enter
demo clock -e hhmm 2142
demo battery -e level 82 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype 4g -e level 4
demo notifications -e visible false
adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true

timeout 1500 adb shell am instrument -w -r \
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
adb logcat -d -v brief -s DespiertaCapturas:V > "$output_dir/tour-log.txt" 2>&1 || true
# Publica como anotación qué pantallas fallaron y por qué (primera línea de cada error).
grep -A1 "captura_fallida" "$output_dir/tour-log.txt" | grep -v "^--" | head -n 24 \
  | sed 's/^/::warning title=Captura fallida::/' || true
grep "capturadas=" "$output_dir/tour-log.txt" | sed 's/^/::notice title=Recorrido::/' || true

annotate() {
  # Una sola anotación de varias líneas, legible desde la API de GitHub.
  local level="$1" title="$2" file="$3" lines="$4"
  [[ -s "$file" ]] || return 0
  local body
  body="$(tail -n "$lines" "$file" | sed 's/%/%25/g; s/\r//g' | awk 'BEGIN { ORS = "%0A" } { print }')"
  echo "::$level title=$title::$body"
}

echo "Capturas copiadas: $count"
if [[ $count -eq 0 ]] || [[ $status -ne 0 ]]; then
  annotate warning "Instrumentación de capturas" "$output_dir/instrumentation.txt" 45
  adb logcat -d -v brief AndroidRuntime:E TestRunner:V DespiertaCapturas:V '*:S' > "$output_dir/runner-log.txt" 2>&1 || true
  annotate warning "Registro del recorrido" "$output_dir/runner-log.txt" 45
fi
if [[ $count -eq 0 ]]; then
  echo "::error title=Capturas::No se generó ninguna captura."
  exit 1
fi
