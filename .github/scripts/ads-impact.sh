#!/usr/bin/env bash
# Ejecuta AdsImpactCheck (memoria, alarma con banner cargando, sin Internet) y publica el
# resultado como anotaciones legibles desde la API de GitHub.
set -uo pipefail
out="${1:?Directorio de salida}"
mkdir -p "$out/anuncios"
pkg="com.bluelabstudios.despierta"
runner="com.bluelabstudios.despierta.test/androidx.test.runner.AndroidJUnitRunner"
adb logcat -c || true
timeout 600 adb shell am instrument -w -r -e class com.example.despierta.AdsImpactCheck "$runner" > "$out/ads-instrumentation.txt" 2>&1
adb logcat -d -v brief -s DespiertaAnuncios:V > "$out/ads-log.txt" 2>&1 || true
for name in $(adb shell run-as "$pkg" ls files/anuncios 2>/dev/null | tr -d '\r'); do
  adb exec-out run-as "$pkg" cat "files/anuncios/$name" > "$out/anuncios/$name" || rm -f "$out/anuncios/$name"
done
body() { sed 's/%/%25/g; s/\r//g' | awk 'BEGIN{ORS="%0A"}{print}'; }
echo "::notice title=Impacto de la publicidad (resultado)::$(grep -E "OK \(|Tests run|FAILURES|INSTRUMENTATION_STATUS: stack=" "$out/ads-instrumentation.txt" | head -12 | body)"
echo "::notice title=Impacto de la publicidad (registro)::$(grep -E "memoria_pss_kb|alarma_con_anuncios|sin_internet|sdk_|banner_|consent_" "$out/ads-log.txt" | cut -c1-400 | head -30 | body)"
