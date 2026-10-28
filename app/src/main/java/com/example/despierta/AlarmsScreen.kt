package com.example.despierta

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

// Pantalla de Alarmas (2.6), "el día en una línea": un riel vertical recorre el día y ordena
// lo que importa a quien trabaja por turnos: ahora, el descanso estimado y la próxima alarma
// (protagonista), seguida del resto de alarmas. La identidad es la tipografía y el riel.

private val NodeSize = 42.dp
private val SideMargin = 24.dp
private val RailX = SideMargin + NodeSize / 2
private val ContentStart = SideMargin + NodeSize + 18.dp

private fun numerals(size: TextUnit, line: TextUnit, spacing: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = spacing,
    fontFeatureSettings = "tnum"
)

/** Dibuja el tramo del riel que pasa por esta fila (de arriba abajo, o hasta el centro del nodo). */
private fun Modifier.railBehind(
    color: Color,
    startAtCenter: Boolean = false,
    endAtCenter: Boolean = false,
    centerY: Dp? = null
): Modifier = drawBehind {
    val middle = centerY?.toPx() ?: (size.height / 2f)
    val top = if (startAtCenter) middle else 0f
    val bottom = if (endAtCenter) middle else size.height
    drawLine(color, Offset(RailX.toPx(), top), Offset(RailX.toPx(), bottom), strokeWidth = 2.dp.toPx())
}

@Composable
internal fun AlarmHome(
    alarms: List<AlarmModel>,
    nowMillis: Long,
    sleepPlan: SleepPlan,
    animated: Boolean,
    onAddAlarm: () -> Unit,
    onEditAlarm: (AlarmModel) -> Unit,
    onToggleAlarm: (AlarmModel, Boolean) -> Unit,
    onDeleteAlarm: (AlarmModel) -> Unit,
    onOpenSleep: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val next = remember(alarms, nowMillis / 1_000L) { findNextAlarm(alarms, nowMillis) }
    val others = remember(alarms, next) {
        alarms.filter { it.id != next?.alarm?.id }.sortedWith(compareBy({ it.hour }, { it.minute }))
    }
    // Confirmación antes de eliminar: la alarma solo se borra si el usuario lo confirma.
    var pendingDelete by remember { mutableStateOf<AlarmModel?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.sky.last())
            .drawBehind {
                // Resplandor lejano del tema, detrás del riel.
                val radius = 260.dp.toPx()
                val center = Offset(-10.dp.toPx(), 230.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(palette.hero.first().copy(alpha = 0.34f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("alarm_home"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header") { HomeHeader(onAddAlarm = onAddAlarm) }
            item(key = "now") { NowRow(nowMillis = nowMillis) }
            item(key = "rest") {
                RestBand(
                    nowMillis = nowMillis,
                    next = next,
                    sleepPlan = sleepPlan,
                    onOpenSleep = onOpenSleep
                )
            }
            item(key = "next") {
                if (next != null) {
                    NextHero(
                        next = next,
                        nowMillis = nowMillis,
                        animated = animated,
                        isLast = others.isEmpty(),
                        onEdit = { onEditAlarm(next.alarm) },
                        onToggle = { onToggleAlarm(next.alarm, it) },
                        onDelete = { pendingDelete = next.alarm }
                    )
                } else {
                    NoNextRow(hasAlarms = alarms.isNotEmpty(), onAddAlarm = onAddAlarm, isLast = others.isEmpty())
                }
            }
            itemsIndexed(others, key = { _, alarm -> alarm.id }) { index, alarm ->
                AlarmRow(
                    alarm = alarm,
                    isLast = index == others.lastIndex,
                    onEdit = { onEditAlarm(alarm) },
                    onToggle = { onToggleAlarm(alarm, it) },
                    onDelete = { pendingDelete = alarm }
                )
            }
        }
    }

    pendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar alarma?") },
            text = {
                Text(
                    "«${alarm.label}» de las ${formatClockTime(alarm.hour, alarm.minute)} " +
                        "dejará de sonar. Podrás deshacerlo justo después."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteAlarm(alarm)
                    },
                    modifier = Modifier.testTag("confirm_delete")
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
            containerColor = palette.card.first(),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/** Cumbres en el borde superior, la marca y el botón "Nueva alarma". */
@Composable
private fun HomeHeader(onAddAlarm: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    val inset = safeTopInset()
    Box(Modifier.fillMaxWidth().height(inset + 76.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            fun ridge(vararg points: Pair<Float, Float>): Path = Path().apply {
                moveTo(0f, h)
                points.forEach { (x, y) -> lineTo(w * x / 390f, h * y / 120f) }
                lineTo(w, h)
                close()
            }
            drawPath(
                ridge(
                    0f to 70f, 40f to 48f, 70f to 66f, 120f to 22f, 170f to 64f, 205f to 44f,
                    250f to 72f, 300f to 30f, 350f to 62f, 390f to 40f
                ),
                color = lerp(palette.hero.first(), palette.sky.last(), 0.6f).copy(alpha = 0.5f)
            )
            drawPath(
                ridge(
                    0f to 92f, 60f to 70f, 110f to 94f, 170f to 60f, 230f to 92f, 290f to 64f,
                    350f to 94f, 390f to 78f
                ),
                color = palette.sky.last()
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = SideMargin, end = 20.dp, top = inset + 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
            Text(
                "Despierta",
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
                color = palette.textPrimary
            )
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.button)
                    .clickable(onClick = onAddAlarm)
                    .semantics { contentDescription = "Nueva alarma" }
                    .testTag("add_alarm"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.onButton, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** El punto "Ahora": hora y fecha, al principio del riel. */
@Composable
private fun NowRow(nowMillis: Long) {
    val palette = LocalDespiertaPalette.current
    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
    val (time, period) = clockParts(now.hour, now.minute)
    Row(
        Modifier
            .fillMaxWidth()
            .railBehind(Color.White.copy(alpha = 0.32f), startAtCenter = true, centerY = 6.dp + NodeSize / 2)
            .padding(start = SideMargin, end = SideMargin, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        RailNode(ring = Color.White.copy(alpha = 0.5f), dot = Color.White, dotSize = 10.dp)
        Column(Modifier.padding(start = 18.dp).testTag("live_clock")) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    time,
                    style = numerals(44.sp, 46.sp, (-1.5).sp, FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    period,
                    modifier = Modifier.padding(start = 5.dp, bottom = 7.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentSoft
                )
            }
            Text(formatCivilDate(now.toLocalDate()), fontSize = 13.sp, color = palette.textSoft)
        }
    }
}

/** El descanso estimado hasta la próxima alarma: el riel "se comprime" en una banda de noche. */
@Composable
private fun RestBand(
    nowMillis: Long,
    next: AlarmOccurrence?,
    sleepPlan: SleepPlan,
    onOpenSleep: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val remaining = next?.triggerAtMillis?.minus(nowMillis)
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().railBehind(Color.White.copy(alpha = 0.32f))) {
        Box(
            Modifier
                .padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(palette.hero.first().copy(alpha = 0.55f), palette.hero.first().copy(alpha = 0.18f))
                    )
                )
                .clickable(onClick = onOpenSleep)
                .testTag("sleep_duration_card")
        ) {
            Canvas(Modifier.matchParentSize()) {
                var seed = 21
                fun rand(): Float {
                    seed = seed * 1103515245 + 12345
                    return ((seed ushr 8) and 0xFFFF) / 65535f
                }
                repeat(34) {
                    val x = rand() * size.width
                    val y = rand() * size.height
                    val r = (0.5f + rand() * 1.1f) * density
                    drawCircle(Color.White.copy(alpha = 0.2f + rand() * 0.55f), r, Offset(x, y))
                }
                // Luna creciente.
                val c = Offset(30.dp.toPx(), 36.dp.toPx())
                val full = Path().apply { addOval(Rect(c, 14.dp.toPx())) }
                val bite = Path().apply {
                    addOval(Rect(Offset(c.x + 6.dp.toPx(), c.y - 4.dp.toPx()), 12.5f.dp.toPx()))
                }
                drawPath(Path.combine(PathOperation.Difference, full, bite), Color(0xFFFFE2B8))
                // Pinos en el borde inferior derecho.
                val tree = palette.hill.copy(alpha = 0.9f)
                drawPine(size.width * 0.673f, size.height, 36.dp.toPx(), tree)
                drawPine(size.width * 0.757f, size.height, 52.dp.toPx(), tree)
                drawPine(size.width * 0.861f, size.height, 70.dp.toPx(), tree)
                drawPine(size.width * 0.964f, size.height, 48.dp.toPx(), tree)
            }
            Column(Modifier.padding(start = 56.dp, top = 16.dp, end = 52.dp, bottom = 16.dp)) {
                Text("Descanso estimado", fontSize = 14.sp, color = palette.textSoft)
                Text(
                    remaining?.let(::formatSleepDuration) ?: "Sin próxima alarma",
                    style = TextStyle(
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (remaining != null) 30.sp else 24.sp,
                        letterSpacing = (-0.8).sp
                    ),
                    color = Color.White,
                    modifier = Modifier.testTag("sleep_duration_value")
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "dormir a las ${formatMinuteOfDay(sleepPlan.bedMinutes, is24Hour = false)}",
                    fontSize = 13.sp,
                    color = palette.accentSoft
                )
            }
            IconButton(
                onClick = onOpenSleep,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(40.dp)
                    .testTag("open_sleep_planner")
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = "Configurar horas de sueño", tint = palette.textPrimary.copy(alpha = 0.85f))
            }
        }
    }
}

private fun DrawScope.drawPine(cx: Float, baseY: Float, height: Float, color: Color) {
    val half = height * 0.34f
    for (tier in 0 until 4) {
        val top = baseY - height + tier * height * 0.2f
        val bottom = top + height * 0.4f
        val spread = half * (0.45f + tier * 0.22f)
        drawPath(
            Path().apply {
                moveTo(cx, top)
                lineTo(cx + spread, bottom)
                lineTo(cx - spread, bottom)
                close()
            },
            color = color
        )
    }
    drawRect(color, Offset(cx - height * 0.025f, baseY - height * 0.12f), Size(height * 0.05f, height * 0.16f))
}

/** La próxima alarma, protagonista: su hora ocupa el ancho de la pantalla. */
@Composable
private fun NextHero(
    next: AlarmOccurrence,
    nowMillis: Long,
    animated: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val alarm = next.alarm
    val (time, period) = clockParts(alarm.hour, alarm.minute)
    // El halo del nodo "respira" solo con la app a la vista y con las animaciones activadas;
    // en pruebas (animated = false) queda quieto para no bloquear la espera de Compose.
    val glow = if (animated && LocalUiOptions.current.motionFactor > 0f) {
        rememberInfiniteTransition(label = "hero_glow").animateFloat(
            initialValue = 0.16f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "hero_glow_alpha"
        ).value
    } else {
        0.22f
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val numeralWidth = maxWidth - ContentStart - SideMargin - 52.dp
        Row(
            Modifier
                .fillMaxWidth()
                .railBehind(
                    palette.accent.copy(alpha = 0.7f),
                    endAtCenter = isLast,
                    centerY = 14.dp + 16.dp + NodeSize / 2
                )
                .clickable(onClick = onEdit)
                .padding(start = SideMargin, end = SideMargin, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .size(NodeSize)
                    .drawBehind {
                        drawCircle(palette.accent.copy(alpha = glow * 0.55f), radius = size.minDimension / 2 + 16.dp.toPx())
                        drawCircle(palette.accent.copy(alpha = glow), radius = size.minDimension / 2 + 7.dp.toPx())
                    }
                    .clip(CircleShape)
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Alarm, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    FitText(
                        text = time,
                        style = numerals(124.sp, 114.sp, (-7).sp, FontWeight.ExtraBold),
                        maxWidth = numeralWidth,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("alarm_time_${alarm.id}")
                    )
                    Text(
                        period,
                        modifier = Modifier.padding(start = 6.dp, top = 12.dp),
                        style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        color = palette.accent
                    )
                }
                Text(
                    formatTimeUntil(next.triggerAtMillis - nowMillis),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("next_alarm_countdown"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onAccent
                )
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            alarm.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = palette.textPrimary
                        )
                        Text(
                            alarmScheduleLabel(alarm) + if (alarm.mathChallengeEnabled) " · Desafío" else "",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = palette.textSoft
                        )
                    }
                    AlarmActions(alarm = alarm, onEdit = onEdit, onToggle = onToggle, onDelete = onDelete)
                }
            }
        }
    }
}

/** Sin alarma activa (o sin ninguna alarma): el riel termina en una invitación a crear una. */
@Composable
private fun NoNextRow(hasAlarms: Boolean, onAddAlarm: () -> Unit, isLast: Boolean) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .railBehind(Color.White.copy(alpha = 0.22f), endAtCenter = isLast, centerY = 14.dp + NodeSize / 2)
            .clickable(onClick = onAddAlarm)
            .padding(horizontal = SideMargin, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        RailNode(ring = palette.accent.copy(alpha = 0.6f), dot = palette.accent, dotSize = 10.dp)
        Column(Modifier.padding(start = 18.dp)) {
            Text(
                if (hasAlarms) "Sin próxima alarma" else "Tu mañana empieza aquí",
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.6).sp),
                color = palette.textPrimary
            )
            Text(
                if (hasAlarms) "Activa una alarma o crea una nueva." else "Crea una alarma puntual, semanal o por turnos.",
                fontSize = 14.sp,
                color = palette.textSoft,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Una alarma más del riel: nodo, hora grande, nombre y programación. */
@Composable
private fun AlarmRow(
    alarm: AlarmModel,
    isLast: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val active = alarm.enabled || alarm.snoozedUntilMillis != null
    val contentAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.42f,
        animationSpec = tween(motion(200)),
        label = "alarm_row_alpha"
    )
    val (time, period) = clockParts(alarm.hour, alarm.minute)
    Row(
        Modifier
            .fillMaxWidth()
            .railBehind(Color.White.copy(alpha = 0.22f), endAtCenter = isLast)
            .clickable(onClick = onEdit)
            .padding(start = SideMargin, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RailNode(
            ring = if (active) palette.accent else Color.White.copy(alpha = 0.3f),
            dot = if (active) palette.accent else Color.White.copy(alpha = 0.5f),
            dotSize = if (active) 10.dp else 8.dp,
            modifier = Modifier.alpha(contentAlpha)
        )
        Column(Modifier.weight(1f).padding(start = 18.dp).alpha(contentAlpha)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.testTag("alarm_time_${alarm.id}")) {
                Text(
                    time,
                    style = numerals(34.sp, 36.sp, (-1).sp, FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    period,
                    modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentSoft
                )
            }
            Text(
                listOf(alarm.label, alarmScheduleLabel(alarm)).joinToString(" · ") +
                    if (alarm.mathChallengeEnabled) " · Desafío" else "",
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.textSoft
            )
        }
        AlarmActions(alarm = alarm, onEdit = onEdit, onToggle = onToggle, onDelete = onDelete)
    }
}

/** Interruptor y menú "⋯" (Editar, Eliminar) de una alarma. */
@Composable
private fun AlarmActions(
    alarm: AlarmModel,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = despiertaSwitchColors(),
            modifier = Modifier.testTag("alarm_toggle_${alarm.id}")
        )
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(44.dp).testTag("alarm_more_${alarm.id}")
            ) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "Opciones de ${alarm.label}", tint = palette.textSoft)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = palette.active.first(),
                shape = RoundedCornerShape(16.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = palette.cardIcon) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                    modifier = Modifier.testTag("alarm_edit_${alarm.id}")
                )
                DropdownMenuItem(
                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("alarm_delete_${alarm.id}")
                )
            }
        }
    }
}

/** Nodo del riel: un círculo con anillo y un punto, del color del fondo para "cortar" la línea. */
@Composable
private fun RailNode(ring: Color, dot: Color, dotSize: Dp, modifier: Modifier = Modifier) {
    val palette = LocalDespiertaPalette.current
    Box(
        modifier
            .size(NodeSize)
            .clip(CircleShape)
            .background(palette.sky.last())
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(dotSize).clip(CircleShape).background(dot))
    }
}
