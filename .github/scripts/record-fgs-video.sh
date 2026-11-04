#!/usr/bin/env bash
# Graba en el emulador, sin cortes ni ediciones, el uso real del servicio en primer plano de
# Despierta (FOREGROUND_SERVICE_MEDIA_PLAYBACK): se programa una alarma desde la interfaz, la app
# pasa a segundo plano, la alarma suena con su notificación de servicio y se detiene desde ella.
# Los toques se envían con `adb shell input`, igual que un dedo; la app no recibe ningún atajo.
set -uo pipefail

out="${1:?Directorio de salida}"
pkg="com.bluelabstudios.despierta"
mkdir -p "$out"
log="$out/registro.txt"
say() { echo "[$(date -u +%H:%M:%S)] $*" | tee -a "$log"; }
here="$(cd "$(dirname "$0")" && pwd)"

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml 2>/dev/null; }
find_node() { dump | python3 "$here/ui-find.py" "$@"; }
tap() {  # tap MODO VALOR [índice] [intentos]
  local tries="${4:-15}" xy
  for _ in $(seq 1 "$tries"); do
    if xy="$(find_node "$1" "$2" "${3:-0}")"; then
      say "toque en «$2» ($xy)"; adb shell input tap $xy; return 0
    fi
    sleep 1
  done
  say "NO ENCONTRADO: «$2»"; return 1
}

# Preparación del dispositivo (antes de grabar).
adb shell settings put system time_12_24 24
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell cmd statusbar collapse
adb shell rm -f /sdcard/stoprec '/sdcard/fgs_*.mp4'
say "dispositivo: $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
say "app: $(adb shell dumpsys package $pkg | grep -m2 -E 'versionName|versionCode' | tr -d '\r' | xargs)"

# Grabación continua en tramos de 170 s (límite de screenrecord) hasta que se pida parar.
# --bugreport añade la marca de tiempo del sistema en cada fotograma.
adb shell 'i=0; while [ ! -f /sdcard/stoprec ]; do screenrecord --bugreport --bit-rate 8000000 --time-limit 170 /sdcard/fgs_$i.mp4; i=$((i+1)); done' &
rec=$!
sleep 3
say "grabación iniciada"

adb shell input keyevent KEYCODE_HOME
sleep 2
say "abrir Despierta"
adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
# Permiso de notificaciones de Android 13+ (diálogo del sistema, si aparece).
tap textci "Allow" 0 4 || tap textci "Permitir" 0 2 || true
sleep 2

tap desc "Nueva alarma" || exit 1
sleep 2
tap text "Introducir hora manualmente" || exit 1
sleep 1

# Hora objetivo: el minuto siguiente, o el otro si quedan menos de 35 s.
now="$(adb shell date +%H:%M:%S | tr -d '\r')"
h=$((10#${now:0:2})); m=$((10#${now:3:2})); s=$((10#${now:6:2}))
add=1; [ "$s" -gt 25 ] && add=2
total=$(( (h * 60 + m + add) % 1440 ))
target="$(printf '%02d:%02d' $((total / 60)) $((total % 60)))"
say "hora del dispositivo $now → alarma a las $target"

tap class "android.widget.EditText" 0 || exit 1
sleep 1
adb shell input keyevent KEYCODE_MOVE_END
for _ in $(seq 1 12); do adb shell input keyevent KEYCODE_DEL; done
# La app sigue el formato del sistema: si el campo pide AM/PM, se escribe en 12 horas.
if find_node text "Hora (HH:MM AM/PM)" >/dev/null; then
  th=$((total / 60)); per=AM; [ "$th" -ge 12 ] && per=PM
  th=$((th % 12)); [ "$th" -eq 0 ] && th=12
  typed="$(printf '%02d:%02d %s' "$th" $((total % 60)) "$per")"
else
  typed="$target"
fi
say "se escribe la hora «$typed»"
adb shell input text "${typed// /%s}"
sleep 2
dump > "$out/ui-antes-de-guardar.xml"
tap text "Guardar" || exit 1
sleep 4
dump > "$out/ui-tras-guardar.xml"
adb shell dumpsys alarm | grep -B2 -A6 "$pkg" | head -40 > "$out/alarmas-programadas.txt" || true
say "alarmas del sistema para la app: $(grep -c "$pkg" "$out/alarmas-programadas.txt")"
say "alarma guardada; la app pasa a segundo plano"
adb shell input keyevent KEYCODE_HOME

# Esperar a que suene: el servicio de alarma pasa a primer plano.
rang=false
for _ in $(seq 1 180); do
  if adb shell dumpsys activity services "$pkg" 2>/dev/null | grep -q "AlarmService"; then rang=true; break; fi
  sleep 1
done
if [ "$rang" != true ]; then say "LA ALARMA NO SONÓ"; touch_stop=1; fi
say "la alarma está sonando: $rang"
sleep 4
adb shell dumpsys activity services "$pkg" > "$out/servicio-sonando.txt" 2>&1
adb shell dumpsys notification --noredact 2>/dev/null | grep -A25 "pkg=$pkg" > "$out/notificacion-sonando.txt" || true
grep -E "AlarmService|isForeground|foregroundServiceType|types=" "$out/servicio-sonando.txt" | head -8 | tee -a "$log"

sleep 4
say "abrir la barra de notificaciones"
adb shell cmd statusbar expand-notifications
sleep 4
tap textci "Detener" 0 8 || tap textci "DETENER" 0 2 || true
sleep 4
if adb shell dumpsys activity services "$pkg" 2>/dev/null | grep -q "AlarmService"; then
  say "EL SERVICIO SIGUE ACTIVO TRAS DETENER"
else
  say "alarma detenida: el servicio en primer plano terminó"
fi
adb shell cmd statusbar collapse
sleep 3

adb shell touch /sdcard/stoprec
adb shell pkill -INT screenrecord
wait "$rec" 2>/dev/null
sleep 2
for f in $(adb shell ls /sdcard/ | tr -d '\r' | grep -E '^fgs_[0-9]+\.mp4$' | sort -V); do
  adb pull "/sdcard/$f" "$out/$f" >/dev/null 2>&1 && say "tramo $f: $(stat -c %s "$out/$f") bytes"
done
adb logcat -d -v time -s DespiertaAlarm:* > "$out/logcat-alarma.txt" 2>&1 || true
[ "$rang" = true ]
