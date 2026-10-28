# Registro de cambios de Despierta

Cada versión tiene un `versionName` y un `versionCode` único y creciente en
`app/build.gradle.kts`. CI toma la versión de ahí para nombrar los APK
(`Despierta-<versión>.apk`, `Despierta-<versión>-fluida.apk`) y el artefacto
(`Despierta-<versión>`). Cada versión estable queda marcada con una etiqueta de git
(`v2.5`, `v2.6`...) para poder volver a ella.

## 2.6 — versionCode 9 — Rediseño de la pantalla principal (en revisión)

Solo cambia la pantalla de Alarmas y lo común que necesita. Sueño, Turnos, Ajustes, el editor
y la alarma sonando se rediseñan en la 2.7.

### Alarmas
- Paisaje nocturno a sangre en la cabecera (cielo, resplandor, tres cordilleras, pinos y
  estrellas) con los colores de cada tema; la luna y el paisaje se desplazan con un paralaje
  discreto.
- "DESPIERTA" y el botón "+" sobre el cielo, sin tarjeta.
- Reloj grande en una tarjeta superpuesta al paisaje, con cifras de ancho fijo.
- Próxima alarma como fila compacta con cuenta atrás legible ("en 7 h 33 min").
- Descanso estimado en una tarjeta compacta con el horario de sueño.
- Alarmas en una lista agrupada: hora, AM/PM, nombre, programación e interruptor; Editar y
  Eliminar en el menú "⋯" (con confirmación y deshacer, como antes).

### Común
- La app dibuja detrás de la barra de estado: desaparece la franja negra superior.

Sin cambios en el motor de alarmas ni en la programación.

## 2.5 — versionCode 8 — estable (etiqueta `v2.5`)

### Interfaz
- Rediseño nocturno: fondo con degradado, tarjetas redondeadas, luna con estrellas y
  paisaje en la cabecera.
- Temas Noche, Amanecer y Bosque, colores de acento, iconos de navegación, iconos de
  tarjetas y botones; estilo de iconos, texto grande, nivel de animaciones y restaurar
  apariencia.
- Formato de 12 horas con AM/PM en toda la app.
- Sueño sin reloj circular: dormir y despertar, duración estimada, objetivo con barra de
  progreso, días de la semana y recordatorio.
- Turnos con calendario mensual de días de trabajo, descanso y excepciones.
- Ajustes organizados en categorías con subpantallas.
- Confirmación antes de eliminar una alarma y opción de deshacer.
- Barra de pestañas al estilo iOS y animaciones que respetan la reducción de movimiento.

### Funciones
- Notificaciones opcionales: recordatorio antes de dormir y aviso de próxima alarma, con
  anticipación de 5, 10, 15 o 30 minutos. Módulo separado del motor de alarmas.
- Repetición predeterminada para alarmas nuevas.

### Correcciones
- La rueda de selección guardaba a veces una hora menos (12 → 11) cuando el ajuste final
  se quedaba unos píxeles corto. Ahora elige la fila centrada.
- Texto negro sobre fondos oscuros en el editor y los diálogos.

### Rendimiento
- Pide la frecuencia de pantalla más alta disponible (90 o 120 Hz).
- El reloj ya no recalcula toda la pantalla cada segundo.
- APK optimizado `-fluida` (R8, sin depuración).

### Desarrollo
- Capturas automáticas en la rama `screenshots` (emulador Pixel 6, datos ficticios).
- Interfaz dividida en archivos por pantalla, componentes comunes y
  `AlarmEditorState` para el estado y las validaciones del editor.

No se modificó el motor de alarmas (`AlarmScheduler`, `AlarmReceiver`, `AlarmService`,
`AlarmStore`, `BootReceiver`).
