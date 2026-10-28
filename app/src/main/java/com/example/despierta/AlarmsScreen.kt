package com.example.despierta

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Pantalla de Alarmas (2.6): paisaje integrado en la cabecera, reloj superpuesto, próxima
// alarma compacta, descanso estimado y lista agrupada de alarmas.

/** Cifras de ancho fijo: la hora no "baila" al cambiar los dígitos. */
private val TabularFigures = TextStyle(fontFeatureSettings = "tnum")

/** Altura del paisaje y punto donde el reloj se superpone a él. */
private val SceneHeight = 330.dp
private val ClockOverlap = 258.dp

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
    val motionOn = LocalUiOptions.current.motionFactor > 0f
    val next = remember(alarms, nowMillis / 1_000L) { findNextAlarm(alarms, nowMillis) }
    // Confirmación antes de eliminar: la alarma solo se borra si el usuario lo confirma.
    var pendingDelete by remember { mutableStateOf<AlarmModel?>(null) }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("alarm_home"),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(DespiertaDimens.ItemSpacing)
    ) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth()) {
                // El paisaje se desplaza más despacio que la lista (paralaje discreto).
                NightScene(
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SceneHeight)
                        .graphicsLayer {
                            if (motionOn && listState.firstVisibleItemIndex == 0) {
                                translationY = listState.firstVisibleItemScrollOffset * 0.45f
                            }
                        }
                )
                MoonWithStars(
                    modifier = Modifier
                        .padding(top = safeTopInset() + 40.dp, end = 104.dp)
                        .size(84.dp)
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            if (motionOn && listState.firstVisibleItemIndex == 0) {
                                translationY = listState.firstVisibleItemScrollOffset * 0.6f
                            }
                        },
                    animated = animated
                )
                HomeTopBar(onAddAlarm = onAddAlarm)
                Box(
                    Modifier
                        .padding(top = ClockOverlap)
                        .padding(horizontal = DespiertaDimens.ScreenPadding)
                ) {
                    ClockCard(nowMillis = nowMillis, animated = animated)
                }
            }
        }
        item(key = "next") {
            NextAlarmRow(
                next = next,
                nowMillis = nowMillis,
                onClick = { next?.let { onEditAlarm(it.alarm) } ?: onAddAlarm() },
                modifier = Modifier.padding(horizontal = DespiertaDimens.ScreenPadding)
            )
        }
        item(key = "rest") {
            RestCard(
                nowMillis = nowMillis,
                next = next,
                sleepPlan = sleepPlan,
                onOpenSleep = onOpenSleep,
                modifier = Modifier.padding(horizontal = DespiertaDimens.ScreenPadding)
            )
        }
        item(key = "heading") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "Alarmas",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when (val active = alarms.count { it.enabled }) {
                        0 -> "Ninguna activa"
                        1 -> "1 activa"
                        else -> "$active activas"
                    },
                    fontSize = 13.sp,
                    color = palette.textSoft
                )
            }
        }
        item(key = "list") {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = DespiertaDimens.ScreenPadding)
                    .fillMaxWidth()
                    .animateContentSize(tween(motionMillis(220, motionOn)))
            ) {
                if (alarms.isEmpty()) {
                    EmptyAlarmsRow(onAddAlarm = onAddAlarm)
                } else {
                    alarms.forEachIndexed { index, alarm ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(start = 18.dp)
                            )
                        }
                        AlarmRow(
                            alarm = alarm,
                            onEdit = { onEditAlarm(alarm) },
                            onToggle = { onToggleAlarm(alarm, it) },
                            onDelete = { pendingDelete = alarm }
                        )
                    }
                }
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

private fun motionMillis(base: Int, on: Boolean): Int = if (on) base else 0

/** Marca y botón "Nueva alarma" sobre el cielo, sin tarjeta. */
@Composable
private fun HomeTopBar(onAddAlarm: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = safeTopInset() + 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "DESPIERTA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                color = palette.textPrimary
            )
            Text("Tu tiempo, bajo control", color = palette.textPrimary.copy(alpha = 0.72f), fontSize = 13.sp)
        }
        Box(
            Modifier
                .size(48.dp)
                .glass(CircleShape, Color.White.copy(alpha = 0.10f), elevation = 10.dp)
                .clickable(onClick = onAddAlarm)
                .semantics { contentDescription = "Nueva alarma" }
                .testTag("add_alarm"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

/** Reloj principal superpuesto al paisaje, con su propio segundero. */
@Composable
private fun ClockCard(nowMillis: Long, animated: Boolean) {
    val palette = LocalDespiertaPalette.current
    var secondMillis by remember { mutableLongStateOf(nowMillis) }
    DisposableEffect(animated) {
        if (!animated) return@DisposableEffect onDispose { }
        val handler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                secondMillis = System.currentTimeMillis()
                handler.postDelayed(this, (1_000L - secondMillis % 1_000L).coerceAtLeast(50L))
            }
        }
        handler.post(ticker)
        onDispose { handler.removeCallbacks(ticker) }
    }
    val shown = if (animated) secondMillis else nowMillis
    val now = Instant.ofEpochMilli(shown).atZone(ZoneId.systemDefault())
    val (time, period) = clockParts(now.hour, now.minute)
    val shape = RoundedCornerShape(DespiertaDimens.HeroCorner)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape, palette.card.last().copy(alpha = 0.70f), elevation = 22.dp)
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(
            formatCivilDate(now.toLocalDate()),
            color = palette.textSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("live_clock")
        ) {
            Text(
                time,
                fontSize = 68.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1.5).sp,
                style = TabularFigures,
                color = palette.textPrimary
            )
            Column(Modifier.padding(start = 10.dp)) {
                Text(period, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.accentSoft)
                Text(
                    now.format(DateTimeFormatter.ofPattern(":ss")),
                    fontSize = 15.sp,
                    style = TabularFigures,
                    color = palette.textSoft
                )
            }
        }
    }
}

/** Próxima alarma como fila de lista: icono, nombre, cuándo y cuánto falta. */
@Composable
private fun NextAlarmRow(
    next: AlarmOccurrence?,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(Icons.Rounded.Alarm, size = 42.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    next?.alarm?.label ?: "Sin próxima alarma",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.textPrimary
                )
                Text(
                    next?.let { formatOccurrence(it.triggerAtMillis) } ?: "Crea o activa una alarma",
                    fontSize = 13.sp,
                    color = palette.textSoft
                )
            }
            if (next != null) {
                Text(
                    formatTimeUntil(next.triggerAtMillis - nowMillis),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentSoft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("next_alarm_countdown")
                )
            } else {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
            }
        }
    }
}

/** Descanso estimado hasta la próxima alarma y horario de sueño, en una tarjeta compacta. */
@Composable
private fun RestCard(
    nowMillis: Long,
    next: AlarmOccurrence?,
    sleepPlan: SleepPlan,
    onOpenSleep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val remaining = next?.triggerAtMillis?.minus(nowMillis)
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_duration_card"),
        onClick = onOpenSleep
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Descanso estimado", fontSize = 13.sp, color = palette.textSoft)
                Text(
                    remaining?.let(::formatSleepDuration) ?: "Sin próxima alarma",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    modifier = Modifier.testTag("sleep_duration_value")
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = DuskBlue, modifier = Modifier.size(16.dp))
                    Text(
                        " ${formatMinuteOfDay(sleepPlan.bedMinutes, is24Hour = false)}",
                        fontSize = 13.sp,
                        color = palette.textPrimary
                    )
                    Text("  →  ", fontSize = 13.sp, color = palette.textSoft)
                    Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = DawnOrange, modifier = Modifier.size(16.dp))
                    Text(
                        " ${formatMinuteOfDay(sleepPlan.wakeMinutes, is24Hour = false)}",
                        fontSize = 13.sp,
                        color = palette.textPrimary
                    )
                }
            }
            IconButton(
                onClick = onOpenSleep,
                modifier = Modifier
                    .size(42.dp)
                    .glass(RoundedCornerShape(14.dp), Color.White.copy(alpha = 0.06f), elevation = 4.dp, sheen = 0.8f)
                    .testTag("open_sleep_planner")
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = "Configurar horas de sueño", tint = palette.cardIcon)
            }
        }
    }
}

/** Fila compacta de alarma dentro de la lista agrupada. */
@Composable
private fun AlarmRow(
    alarm: AlarmModel,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val active = alarm.enabled || alarm.snoozedUntilMillis != null
    val contentAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.5f,
        animationSpec = tween(motion(200)),
        label = "alarm_row_alpha"
    )
    var menuOpen by remember { mutableStateOf(false) }
    val (time, period) = clockParts(alarm.hour, alarm.minute)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).alpha(contentAlpha)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.testTag("alarm_time_${alarm.id}")) {
                Text(
                    time,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Medium,
                    style = TabularFigures,
                    color = palette.textPrimary
                )
                Text(
                    period,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentSoft,
                    modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
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

@Composable
private fun EmptyAlarmsRow(onAddAlarm: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddAlarm)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(palette.button.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AlarmAdd, contentDescription = null, tint = palette.button)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("Tu mañana empieza aquí", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text("Crea una alarma puntual, semanal o por turnos.", fontSize = 13.sp, color = palette.textSoft)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
    }
}
