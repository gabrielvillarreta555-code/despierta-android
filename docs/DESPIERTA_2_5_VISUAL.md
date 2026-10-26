# Despierta 2.5 — identidad visual recuperada

Despierta 2.5 parte del commit funcional de Despierta 2.4 y mantiene sin cambios el
programador, receptor, servicio y almacenamiento de alarmas.

## Cambios visibles

- Paleta coherente azul noche, celeste suave y lavanda en la pantalla principal,
  editor, turnos, ajustes y pantalla de alarma activa.
- Tarjeta de descanso estimado calculada desde la hora actual hasta la siguiente alarma.
- Mensajes breves para preparar el descanso, sin sustituir recomendaciones médicas.
- Versión visible 2.5 (`versionCode` 8).

## Artefactos

GitHub Actions publica el APK de prueba y el AAB release sin firma dentro del artefacto
`Despierta-2.5`. El AAB sigue necesitando la clave de carga privada antes de enviarlo a
Google Play; ninguna clave se incluye en el repositorio.

## Rediseño nocturno (rama `feature/redisenio-nocturno`)

- Fondo con degradado violeta, tarjetas redondeadas con borde de luz y paleta lavanda.
- Luna creciente con estrellas dibujada con Canvas (sin imágenes) en la pantalla principal y
  en la pantalla de alarma activa; se dibuja estática cuando las pruebas desactivan el reloj vivo.
- Reloj principal grande con segundos, tarjeta de descanso estimado con acceso al plan de sueño.
- Nueva sección **Sueño**: dial de 24 horas, tarjetas Dormir/Despertar, selector de ruedas y
  botón para crear la alarma de despertar. El botón abre el editor de siempre; el plan solo se
  guarda como preferencia visual en `despierta_ui` y nunca programa alarmas por sí mismo.
- Barra inferior flotante en forma de píldora; interruptores con pista lavanda.
- Sin cambios en `AlarmScheduler`, `AlarmReceiver`, `AlarmService`, `AlarmStore`,
  `BootReceiver`, permisos ni manifiesto.
