#!/usr/bin/env bash
# Validación visual "antes y después": instala la APK base y la candidata, una tras otra, en el
# mismo emulador y con las mismas condiciones, y ejecuta VisualValidation con cada una.
# Se repite con una segunda configuración de pantalla parecida a un teléfono de 1080x2392 a
# 440 dpi (393 dp de ancho), además del Pixel 6 (1080x2400, 420 dpi, 411 dp).
set -uo pipefail

out="${1:?Directorio de salida}"
apk_before="${2:?APK base}"
apk_after="${3:?APK candidata}"
apk_tests="${4:?APK de pruebas}"
only_config="${5:-}"
mkdir -p "$out"

pkg="com.bluelabstudios.despierta"
runner="com.bluelabstudios.despierta.test/androidx.test.runner.AndroidJUnitRunner"

demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null 2>&1 || true; }

adb shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true
# La pantalla no debe apagarse entre instalaciones: con la pantalla apagada las capturas salen negras.
adb shell svc power stayon true >/dev/null 2>&1 || true
adb shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
adb shell settings put global sysui_demo_allowed 1 >/dev/null 2>&1 || true
demo enter
demo clock -e hhmm 2142
demo battery -e level 82 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype 4g -e level 4
demo notifications -e visible false

bash .github/scripts/install-apk-with-retry.sh "$apk_tests" "$out/install-tests.txt" || exit 1

run_with() {
  local config="$1" version="$2" apk="$3"
  adb uninstall "$pkg" >/dev/null 2>&1 || true
  bash .github/scripts/install-apk-with-retry.sh "$apk" "$out/install-$config-$version.txt" || return 1
  adb shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell dumpsys power | grep -m1 -E "mWakefulness=" >> "$out/pantalla-$config-$version.txt" 2>&1 || true
  adb logcat -c || true
  timeout 600 adb shell am instrument -w -r -e etiqueta "$config-$version" \
    -e class com.example.despierta.VisualValidation,com.example.despierta.StarsAnimationCheck "$runner" > "$out/instrument-$config-$version.txt" 2>&1
  for name in $(adb shell run-as "$pkg" ls files/validacion 2>/dev/null | tr -d '\r'); do
    [[ "$name" == *.png ]] || continue
    adb exec-out run-as "$pkg" cat "files/validacion/$name" > "$out/$name" || rm -f "$out/$name"
  done
  adb logcat -d -v brief -s DespiertaValidacion:V > "$out/log-$config-$version.txt" 2>&1 || true
  grep -h "validacion_fallida" -A1 "$out/log-$config-$version.txt" | head -n 6 \
    | sed "s/^/::warning title=Validación $config-$version::/" || true
  grep -c "OK (2 tests)" "$out/instrument-$config-$version.txt" >/dev/null \
    || echo "::warning title=Validación $config-$version::$(tail -n 3 "$out/instrument-$config-$version.txt" | tr '\n' ' ')"
}

for config in pixel6 telefono; do
  [[ -n "$only_config" && "$config" != "$only_config" ]] && continue
  if [[ "$config" == "telefono" ]]; then
    adb shell wm size 1080x2392
    adb shell wm density 440
    sleep 5
    # El cambio de pantalla reinicia la barra de estado: se vuelve a fijar el modo demostración.
    demo enter
    demo clock -e hhmm 2142
    demo battery -e level 82 -e plugged false
    demo network -e wifi show -e level 4 -e mobile show -e datatype 4g -e level 4
    demo notifications -e visible false
  fi
  run_with "$config" antes "$apk_before"
  run_with "$config" despues "$apk_after"
done
adb shell wm size reset || true
adb shell wm density reset || true
demo exit

count=$(ls "$out"/*.png 2>/dev/null | wc -l)
echo "::notice title=Validación::$count capturas: $(cd "$out" && ls *.png 2>/dev/null | tr '\n' ' ')"
[[ $count -gt 0 ]]
