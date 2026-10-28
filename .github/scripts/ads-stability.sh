#!/usr/bin/env bash
# Ejecuta cada escenario de AdsStabilityCheck en una instrumentación propia y publica si la
# app sobrevivió al reinicio forzado de Google Play Services con una alarma sonando.
set -uo pipefail
out="${1:?Directorio de salida}"
mkdir -p "$out"
runner="com.bluelabstudios.despierta.test/androidx.test.runner.AndroidJUnitRunner"
body() { sed 's/%/%25/g; s/\r//g' | awk 'BEGIN{ORS="%0A"}{print}'; }
summary=""
for scenario in soloAlarma pantallaCerrada pantallaAbierta; do
  adb logcat -c || true
  # Sin --foreground, timeout deja adb en segundo plano: al leer la terminal se detiene y no escribe nada.
  timeout --foreground 300 adb shell am instrument -w -r -e class "com.example.despierta.AdsStabilityCheck#$scenario" "$runner" < /dev/null > "$out/estabilidad-$scenario.txt" 2>&1
  echo "codigo_salida=$? bytes=$(wc -c < "$out/estabilidad-$scenario.txt") dispositivos=$(adb devices | tr -d "\r\n\t")" >> "$out/estabilidad-$scenario.txt"
  adb logcat -d -v brief > "$out/estabilidad-$scenario-logcat.txt" 2>&1 || true
  if grep -q "OK (1 test)" "$out/estabilidad-$scenario.txt"; then
    result="SOBREVIVE"
  elif grep -q "Process crashed" "$out/estabilidad-$scenario.txt"; then
    result="APP CERRADA POR ANDROID"
  else
    result="FALLO: $(grep -m1 -E "AssertionError|shortMsg" "$out/estabilidad-$scenario.txt" | cut -c1-200)"
  fi
  kill_line="$(grep -m1 -E "Killing .*despierta.*depends on provider" "$out/estabilidad-$scenario-logcat.txt" | cut -c1-300)"
  detail="$(grep -E "DespiertaAnuncios.*(escenario=|sdk_eventos)" "$out/estabilidad-$scenario-logcat.txt" | cut -c1-300 | head -4)"
  raw="$(grep -v "^INSTRUMENTATION_STATUS: \(id\|numtests\|current\|stream\)=" "$out/estabilidad-$scenario.txt" | tail -n 12 | cut -c1-220)"
  summary+="== $scenario: $result"$'\n'"$detail"$'\n'"${kill_line:+motivo: $kill_line}"$'\n'"salida: $raw"$'\n'
done
# Estado del paquete y de las instrumentaciones instaladas, para diagnosticar.
summary+="instrumentaciones: $(adb shell pm list instrumentation | tr -d '\r' | tr '\n' ' ')"$'\n'
echo "::notice title=Estabilidad con reinicio de Play Services::$(printf '%s' "$summary" | body)"
