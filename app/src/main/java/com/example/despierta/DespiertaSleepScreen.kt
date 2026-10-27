package com.example.despierta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

private enum class SleepTarget { BEDTIME, WAKE }

/**
 * Horario de sueño. Solo guarda preferencias y, si el usuario lo pide, un recordatorio opcional.
 * Despierta no registra el sueño: todas las cifras son estimaciones según el horario.
 * Crear la alarma de despertar reutiliza el editor existente, que es quien guarda y programa.
 */
@Composable
internal fun SleepScreen(
    schedule: SleepSchedule,
    notifications: NotificationPrefs,
    canPostNotifications: Boolean,
    alarms: List<AlarmModel>,
    nowMillis: Long,
    hapticsEnabled: Boolean,
    onScheduleChanged: (SleepSchedule) -> Unit,
    onNotificationsChanged: (NotificationPrefs) -> Unit,
    onCreateWakeAlarm: (hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val plan = schedule.plan
    // null: ninguna hora en edición. Se guarda como nombre para sobrevivir a la rotación.
    var editingName by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = editingName?.let { SleepTarget.valueOf(it) }
    var goalExpanded by rememberSaveable { mutableStateOf(false) }
    val wakeHour = plan.wakeMinutes / 60
    val wakeMinute = plan.wakeMinutes % 60
    val matchingAlarm = alarms.firstOrNull {
        it.enabled && it.hour == wakeHour && it.minute == wakeMinute
    }
    val next = findNextAlarm(alarms, nowMillis)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("sleep_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Volver a alarmas", tint = palette.textPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Sueño", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text("Tu horario de descanso", fontSize = 13.sp, color = palette.textSoft)
            }
        }

        // Horas de dormir y despertar.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SleepTimeTile(
                title = "Dormir",
                minutes = plan.bedMinutes,
                icon = Icons.Rounded.Bedtime,
                iconTint = DuskBlue,
                selected = editing == SleepTarget.BEDTIME,
                onClick = {
                    editingName = if (editing == SleepTarget.BEDTIME) null else SleepTarget.BEDTIME.name
                },
                modifier = Modifier.weight(1f).testTag("sleep_bedtime_card")
            )
            SleepTimeTile(
                title = "Despertar",
                minutes = plan.wakeMinutes,
                icon = Icons.Rounded.WbSunny,
                iconTint = DawnOrange,
                selected = editing == SleepTarget.WAKE,
                onClick = {
                    editingName = if (editing == SleepTarget.WAKE) null else SleepTarget.WAKE.name
                },
                modifier = Modifier.weight(1f).testTag("sleep_wake_card")
            )
        }

        AnimatedVisibility(
            visible = editing != null,
            enter = fadeIn(tween(motion(180))) + expandVertically(tween(motion(220))),
            exit = fadeOut(tween(motion(120))) + shrinkVertically(tween(motion(180)))
        ) {
            val target = editing ?: SleepTarget.WAKE
            GlowCard(
                modifier = Modifier.fillMaxWidth().testTag("sleep_time_editor"),
                contentPadding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (target == SleepTarget.BEDTIME) "Hora de dormir" else "Hora de despertar",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { editingName = null }) {
                        Text("Listo", color = palette.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
                val value = if (target == SleepTarget.BEDTIME) plan.bedMinutes else plan.wakeMinutes
                // La clave recrea las ruedas al cambiar entre dormir y despertar.
                key(target) {
                    TimeWheelPicker(
                        hour24 = value / 60,
                        minute = value % 60,
                        is24Hour = false,
                        hapticsEnabled = hapticsEnabled,
                        onTimeChanged = { hour, minute ->
                            val minutes = normalizeMinuteOfDay(hour * 60 + minute)
                            onScheduleChanged(
                                schedule.copy(
                                    plan = if (target == SleepTarget.BEDTIME) {
                                        plan.copy(bedMinutes = minutes)
                                    } else {
                                        plan.copy(wakeMinutes = minutes)
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }

        SleepDurationSummary(schedule)

        SleepDaysCard(schedule = schedule, onScheduleChanged = onScheduleChanged)

        // Opciones agrupadas, como una lista de ajustes.
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            SleepOptionRow(
                icon = Icons.Rounded.Flag,
                title = "Objetivo de sueño",
                value = formatHoursMinutes(schedule.goalMinutes),
                onClick = { goalExpanded = !goalExpanded },
                modifier = Modifier.testTag("sleep_goal_row")
            )
            AnimatedVisibility(visible = goalExpanded) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SLEEP_GOAL_OPTIONS.forEach { goal ->
                        ChoicePill(
                            text = formatHoursMinutes(goal),
                            selected = goal == schedule.goalMinutes,
                            onClick = { onScheduleChanged(schedule.copy(goalMinutes = goal)) },
                            modifier = Modifier.testTag("sleep_goal_option_$goal")
                        )
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 64.dp))
            val reminderOn = notifications.enabled && notifications.bedtimeReminder
            val bed = plan.bedMinutes - notifications.leadMinutes
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = reminderOn,
                        role = Role.Switch,
                        onValueChange = { on ->
                            onNotificationsChanged(
                                if (on) {
                                    notifications.copy(enabled = true, bedtimeReminder = true)
                                } else {
                                    notifications.copy(bedtimeReminder = false)
                                }
                            )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("sleep_reminder_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledIcon(Icons.Rounded.NotificationsActive)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Recordatorio para dormir", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    Text(
                        when {
                            reminderOn && !canPostNotifications ->
                                "Las notificaciones de Android están desactivadas."
                            reminderOn ->
                                "${notifications.leadMinutes} min antes · ${formatMinuteOfDay(bed, is24Hour = false)}"
                            else -> "Un aviso antes de tu hora de dormir"
                        },
                        fontSize = 12.sp,
                        color = if (reminderOn && !canPostNotifications) {
                            androidx.compose.material3.MaterialTheme.colorScheme.error
                        } else {
                            palette.textSoft
                        }
                    )
                }
                Switch(checked = reminderOn, onCheckedChange = null, colors = despiertaSwitchColors())
            }
        }

        // Alarma de despertar: abre el editor de siempre.
        GlowCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            if (matchingAlarm != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Ya tienes «${matchingAlarm.label}» a las ${formatMinuteOfDay(plan.wakeMinutes, is24Hour = false)}.",
                        fontSize = 13.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("sleep_alarm_matched")
                    )
                }
            } else {
                Button(
                    onClick = { onCreateWakeAlarm(wakeHour, wakeMinute) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.button,
                        contentColor = palette.onButton
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sleep_create_alarm")
                ) {
                    Icon(Icons.Rounded.AlarmAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Crear alarma a las ${formatMinuteOfDay(plan.wakeMinutes, is24Hour = false)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (next != null) {
                val nextMinutes = Instant.ofEpochMilli(next.triggerAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .let { it.hour * 60 + it.minute }
                if (nextMinutes != plan.wakeMinutes) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onScheduleChanged(schedule.copy(plan = plan.copy(wakeMinutes = nextMinutes))) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("sleep_use_next_alarm")
                    ) {
                        Text(
                            "Usar mi próxima alarma (${formatMinuteOfDay(nextMinutes, is24Hour = false)})",
                            color = palette.accentSoft
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SleepTimeTile(
    title: String,
    minutes: Int,
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val shape = RoundedCornerShape(22.dp)
    val (time, period) = clockParts(normalizeMinuteOfDay(minutes) / 60, normalizeMinuteOfDay(minutes) % 60)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) palette.activeBrush else palette.cardBrush)
            .border(
                BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) palette.accent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.07f)
                ),
                shape
            )
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StyledIcon(icon, tint = iconTint, size = 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 13.sp, color = palette.textSoft)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(
                period,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentSoft,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Text(
            if (selected) "Editando" else "Toca para cambiar",
            fontSize = 11.sp,
            color = if (selected) palette.accent else palette.textSoft.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SleepDurationSummary(schedule: SleepSchedule) {
    val palette = LocalDespiertaPalette.current
    val duration = schedule.durationMinutes
    val progress by animateFloatAsState(
        targetValue = schedule.goalProgress,
        animationSpec = tween(motion(320)),
        label = "sleep_goal_progress"
    )
    val percent = (schedule.goalProgress * 100).toInt()
    GlowCard(
        modifier = Modifier.fillMaxWidth().testTag("sleep_duration_summary"),
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Descanso estimado", fontSize = 12.sp, color = palette.textSoft)
                Text(
                    formatHoursMinutes(duration),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.testTag("sleep_duration_value")
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Objetivo", fontSize = 12.sp, color = palette.textSoft)
                Text(
                    formatHoursMinutes(schedule.goalMinutes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
                .semantics { contentDescription = "$percent % del objetivo de sueño" }
                .testTag("sleep_goal_progress")
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(50))
                    .background(palette.accentBrush)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$percent % de tu objetivo · ${sleepGuidance(duration)}",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = palette.textSoft
        )
        Text(
            "Estimación según tu horario. Despierta no registra tu sueño.",
            fontSize = 11.sp,
            color = palette.textSoft.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SleepDaysCard(schedule: SleepSchedule, onScheduleChanged: (SleepSchedule) -> Unit) {
    val palette = LocalDespiertaPalette.current
    GlowCard(modifier = Modifier.fillMaxWidth().testTag("sleep_days"), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Días",
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                when (schedule.days.size) {
                    7 -> "Todos los días"
                    0 -> "Ningún día"
                    else -> "${schedule.days.size} días"
                },
                fontSize = 12.sp,
                color = palette.textSoft
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEK_DAYS_MONDAY_FIRST.forEach { (day, letter) ->
                val on = day in schedule.days
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (on) palette.button else Color.White.copy(alpha = 0.07f))
                        .toggleable(
                            value = on,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                onScheduleChanged(
                                    schedule.copy(days = if (checked) schedule.days + day else schedule.days - day)
                                )
                            }
                        )
                        .testTag("sleep_day_$day"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) palette.onButton else palette.textSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepOptionRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Text(value, color = palette.textSoft, fontSize = 14.sp)
    }
}

/** "8 h" o "7 h 30 min". */
internal fun formatHoursMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0) "$hours h" else "$hours h $minutes min"
}

internal fun formatMinuteOfDay(minutes: Int, is24Hour: Boolean): String {
    val value = normalizeMinuteOfDay(minutes)
    return formatManualTime(value / 60, value % 60, is24Hour)
}

internal fun sleepGuidance(durationMinutes: Int): String = when {
    durationMinutes == 0 -> "Elige horas distintas para dormir y despertar."
    durationMinutes < 7 * 60 ->
        "Descanso corto: prepara el ambiente y reduce distracciones antes de dormir."
    durationMinutes <= 9 * 60 ->
        "Una ventana dentro de la referencia general de 7 a 9 horas para adultos."
    else -> "Ventana larga: útil para recuperarte tras una rotación de turnos."
}
