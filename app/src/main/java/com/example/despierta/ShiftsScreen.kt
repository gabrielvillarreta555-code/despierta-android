package com.example.despierta

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Pantalla de Turnos: calendario mensual y alarmas por turnos.

@Composable
internal fun ShiftScreen(
    alarms: List<AlarmModel>,
    nowMillis: Long,
    onAddShiftAlarm: () -> Unit,
    onEditAlarm: (AlarmModel) -> Unit,
    onToggleAlarm: (AlarmModel, Boolean) -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val shiftAlarms = alarms.filter { it.shiftPattern != null }
    val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("shift_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Turnos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text("Tu ciclo de trabajo en el calendario", color = palette.textSoft, fontSize = 13.sp)
            }
        }
        item { ShiftCalendar(shiftAlarms = shiftAlarms, today = today) }
        items(shiftAlarms, key = AlarmModel::id) { alarm ->
            ShiftAlarmCard(
                alarm = alarm,
                nowMillis = nowMillis,
                onEdit = { onEditAlarm(alarm) },
                onToggle = { onToggleAlarm(alarm, it) }
            )
        }
        item {
            AddButton(
                label = if (shiftAlarms.isEmpty()) "Crear turno" else "Nuevo turno",
                tag = "add_shift",
                onClick = onAddShiftAlarm
            )
        }
    }
}

/** Calendario mensual: trabajo, descanso y excepciones con colores distintos. */
@Composable
private fun ShiftCalendar(shiftAlarms: List<AlarmModel>, today: LocalDate) {
    val palette = LocalDespiertaPalette.current
    var monthOffset by rememberSaveable { mutableStateOf(0) }
    var selectedAlarmId by rememberSaveable { mutableStateOf(-1) }
    var selectedEpochDay by rememberSaveable { mutableStateOf(-1L) }

    val shiftAlarm = shiftAlarms.firstOrNull { it.id == selectedAlarmId }
        ?: shiftAlarms.firstOrNull { it.enabled }
        ?: shiftAlarms.firstOrNull()
    val pattern = shiftAlarm?.shiftPattern
    val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailing = (7 - days.size % 7) % 7
    val weeks = (days + List(trailing) { null }).chunked(7)
    val selectedDate = if (selectedEpochDay >= 0L) LocalDate.ofEpochDay(selectedEpochDay) else null
    val monthTitle = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH) else it.toString() }

    GlowCard(
        modifier = Modifier.fillMaxWidth().testTag("shift_calendar"),
        corner = 24.dp,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthOffset-- }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior", tint = palette.textSoft)
            }
            Text(
                monthTitle,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            IconButton(onClick = { monthOffset++ }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Mes siguiente", tint = palette.textSoft)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = palette.textSoft
                )
            }
        }

        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        ShiftDayCell(
                            date = date,
                            pattern = pattern,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            onClick = { selectedEpochDay = date.toEpochDay() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarLegend(color = palette.accent, text = "Trabajo")
            CalendarLegend(color = DawnOrange, text = "Descanso")
            CalendarLegend(color = MaterialTheme.colorScheme.errorContainer, text = "Libre")
        }

        if (shiftAlarms.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shiftAlarms.forEach { alarm ->
                    FilterChip(
                        selected = alarm.id == shiftAlarm?.id,
                        onClick = { selectedAlarmId = alarm.id },
                        label = { Text(alarm.label) }
                    )
                }
            }
        }

        Text(
            when {
                shiftAlarms.isEmpty() -> "Crea un turno para ver tus días de trabajo y descanso."
                selectedDate != null -> shiftDetail(selectedDate, pattern, shiftAlarm)
                else -> "Toca un día para ver su detalle."
            },
            color = palette.textSoft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 10.dp).testTag("shift_day_detail")
        )
    }
}

@Composable
private fun CalendarLegend(color: Color, text: String) {
    val palette = LocalDespiertaPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = palette.textSoft)
    }
}

@Composable
private fun ShiftDayCell(
    date: LocalDate,
    pattern: ShiftPattern?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val kind = pattern?.let { shiftDayKind(it, date) }
    val background = when (kind) {
        ShiftDayKind.WORK -> palette.accent.copy(alpha = 0.9f)
        ShiftDayKind.REST -> DawnOrange.copy(alpha = 0.28f)
        ShiftDayKind.EXCEPTION -> MaterialTheme.colorScheme.errorContainer
        else -> Color.White.copy(alpha = 0.04f)
    }
    val foreground = when (kind) {
        ShiftDayKind.WORK -> palette.onAccent
        ShiftDayKind.REST -> Color(0xFFFFE3C4)
        ShiftDayKind.EXCEPTION -> MaterialTheme.colorScheme.onErrorContainer
        else -> palette.textSoft
    }
    val ringColor = when {
        isSelected -> Color.White
        isToday -> palette.accentSoft
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (isSelected || isToday) 1.5.dp else 0.dp, ringColor), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = foreground,
            fontSize = 14.sp,
            fontWeight = if (kind == ShiftDayKind.WORK || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun shiftDetail(day: LocalDate, pattern: ShiftPattern?, alarm: AlarmModel?): String {
    val label = formatCivilDate(day)
    if (pattern == null) return "$label · sin turno"
    return when (shiftDayKind(pattern, day)) {
        ShiftDayKind.WORK -> {
            val time = if (alarm != null && alarm.enabled) formatClockTime(alarm.hour, alarm.minute) else null
            if (time != null) "$label · Trabajo · alarma $time" else "$label · Trabajo"
        }
        ShiftDayKind.REST -> "$label · Descanso"
        ShiftDayKind.EXCEPTION -> "$label · Día libre por excepción"
        ShiftDayKind.BEFORE_CYCLE -> "$label · Antes de iniciar el ciclo"
        ShiftDayKind.INVALID -> "$label · Turno sin configurar"
    }
}

@Composable
private fun ShiftAlarmCard(
    alarm: AlarmModel,
    nowMillis: Long,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val pattern = alarm.shiftPattern ?: return
    val upcoming = remember(alarm, nowMillis / 60_000L) {
        upcomingShiftOccurrences(alarm, count = 3, nowMillis = nowMillis)
    }
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        corner = 22.dp,
        borderColor = if (alarm.enabled) palette.accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f),
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(alarm.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(
                    "%s · %d trabajo / %d descanso".format(
                        formatClockTime(alarm.hour, alarm.minute),
                        pattern.workDays,
                        pattern.restDays
                    ),
                    color = palette.textSoft,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar ${alarm.label}", tint = palette.textSoft)
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle, colors = despiertaSwitchColors())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                upcoming.isNotEmpty() -> "Próximas: " + upcoming.joinToString("  ·  ") { formatCompactOccurrence(it) }
                alarm.enabled -> "No se pudo calcular"
                else -> "Alarma pausada"
            },
            color = if (alarm.enabled) palette.accent else palette.textSoft,
            fontSize = 12.sp
        )
    }
}
