# Registro de cambios de Despierta

Cada versión tiene un `versionName` y un `versionCode` único y creciente en
`app/build.gradle.kts`. CI toma la versión de ahí para nombrar los APK
(`Despierta-<versión>.apk`, `Despierta-<versión>-fluida.apk`) y el artefacto
(`Despierta-<versión>`). Cada versión estable queda guardada en una rama
`estable/<versión>` (por ejemplo `estable/2.5`) para poder volver a ella.

## 2.6 — versionCode 9 — Rediseño de la pantalla principal (en revisión)

Solo cambia la pantalla de Alarmas y la barra de navegación. Sueño, Turnos, Ajustes, el
editor y la alarma sonando se rediseñan en la 2.7.

### Alarmas: "el día en una línea"
- Un riel vertical recorre la pantalla y ordena lo que importa a quien trabaja por turnos:
  ahora, descanso estimado y próxima alarma, seguidas del resto de alarmas.
- La próxima alarma es la protagonista: su hora ocupa el ancho de la pantalla (se ajusta
  sola si no cabe), con la cuenta atrás ("en 7 h 33 min") y su nombre y programación.
- "Ahora" con hora y fecha. El descanso estimado es una banda de noche (luna, estrellas y
  pinos) con la hora de dormir.
- Cumbres con los colores del tema en el borde superior; la marca y el botón "+" redondo.
- Resto de alarmas ordenadas por hora, con interruptor y menú "⋯" (Editar y Eliminar, con
  confirmación y deshacer, como antes).
- Tipografía propia para cifras y títulos: Bricolage Grotesque (licencia OFL, en
  `assets/licenses`). El texto corrido sigue con la fuente del sistema.

### Navegación
- Píldora de pestañas: la activa se abre en una cápsula con su nombre y el color de
  navegación elegido en Ajustes; las demás muestran solo el icono.

### Común
- La app dibuja detrás de la barra de estado: desaparece la franja negra superior.
- Margen superior seguro (barra de estado y recorte de cámara) para las cabeceras.

Sin cambios en el motor de alarmas ni en la programación.

## 2.5 — versionCode 8 — estable (rama `estable/2.5`)

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
