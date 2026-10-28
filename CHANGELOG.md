# Registro de cambios de Despierta

Cada versión tiene un `versionName` y un `versionCode` único y creciente en
`app/build.gradle.kts`. CI toma la versión de ahí para nombrar los APK
(`Despierta-<versión>.apk`, `Despierta-<versión>-fluida.apk`) y el artefacto
(`Despierta-<versión>`). Cada versión estable queda guardada en una rama
`estable/<versión>` (por ejemplo `estable/2.5`) para poder volver a ella.

## 2.7 — versionCode 10 — Personalización: paisaje, letra, iconos y fluidez

Incluye la 2.6 (diseño verde) y sus correcciones. Sin cambios en el motor de alarmas.

### Nuevo en Ajustes
- **Apariencia → Paisaje:** Bosque (el de siempre), Montaña (cumbres con nieve, sin pinos) y
  Noche estrellada (más estrellas y vía láctea, colinas bajas). Con vista previa de cada uno.
- **Apariencia → Tipo de letra:** Sistema, Moderna (Outfit), Redondeada (Nunito) y Clásica
  (Lora), para toda la app. Fuentes con licencia OFL incluidas en `assets/licenses`.
- **Apariencia → Tamaño y forma de los iconos:** pequeños, normales o grandes; fondo
  redondeado, en círculo o cuadrado. Se suman al estilo de iconos que ya existía.
- **Accesibilidad → Fluidez de pantalla:** máxima (90/120 Hz si el teléfono lo admite),
  60 Hz para ahorrar batería, o que decida Android. Se aplica al momento.

### Correcciones que vienen de la revisión de la 2.6
- Paisaje: el suelo cercano tapa la base de los pinos; desaparecen las barras oscuras de los
  troncos bajo la copa (se veían en el teléfono y en el emulador).
- El reloj con letra fina ya no hace crecer su tarjeta.

### Desarrollo
- Validación visual "antes y después" en ramas `validacion/**` y auditoría de privacidad en
  ramas `auditoria/**`. El recorrido de capturas incluye los paisajes, la letra y los ajustes
  nuevos.

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

### Cristal (revisión tras la primera prueba en el teléfono)
- Superficies de cristal al estilo iOS: relleno translúcido con luz desde arriba, reflejo en la
  esquina, borde con brillo (claro arriba-izquierda, reflejo abajo-derecha) y sombra suave.
  Ya no son tarjetas planas con el mismo contorno de siempre.
- Reloj, próxima alarma, descanso, lista de alarmas y botón "+" (ahora circular) en cristal.
- Barra de navegación como píldora de cristal flotante, con la pestaña activa en relieve.
  Es común a todas las pantallas; el resto de tarjetas pasa a cristal en la 2.7.
- Paisaje: neblina entre planos y pinos que se disuelven hacia abajo (sin cortes ni troncos
  sueltos), pinos algo menos oscuros.

### Común
- La app dibuja detrás de la barra de estado: desaparece la franja negra superior.
- Margen superior seguro (barra de estado y recorte de cámara) para las cabeceras, con un
  mínimo cuando el sistema no informa del espacio.

### Diseño restaurado
- Se probó otra composición ("el día en una línea": riel vertical, tipografía Bricolage,
  navegación en cápsula) y se descartó. Alarmas vuelve al diseño verde (commit `93ff179`).
  La prueba queda guardada en la rama `ideas/alarmas-riel`.
- Se conserva de esa etapa: el recorrido de capturas detecta capturas negras y reintenta una
  sesión que arranca sin interfaz.

### Refinamientos sobre el diseño verde (sin cambiar la composición)
- Bordes de cristal más finos (1 dp) y con menos brillo; reflejo interior más suave.
- Las tarjetas se hunden ligeramente al tocarlas (sin animación si se reduce el movimiento).
- Interruptores apagados del color del tema, sin el violeta fijo ni contorno.
- Iconos con relieve suave teñido de su propio color y esquinas algo más redondas.
- Paisaje: los troncos ya no bajan como barras por los bordes de la pantalla.
- Descanso sin alarma activa dice "Activa una alarma" en vez de repetir "Sin próxima alarma".

### Pendiente (pedido, sin planificar todavía)
- Más ajustes para los iconos.
- Configuración de FPS (frecuencia de pantalla).
- Más tipos de letra a elegir.
- Fondos a elegir: bosque, montaña, noche con estrellas (el actual sigue como predeterminado).

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
