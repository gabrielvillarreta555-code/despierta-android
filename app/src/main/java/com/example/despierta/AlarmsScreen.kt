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

// Pantalla de Alarmas: reloj, descanso estimado y lista de alarmas.

@Composable
internal fun AlarmHome(
    alarms: List<AlarmModel>,
    nowMillis: Long,
    sleepPlan: SleepPlan,
    animated: Boolean,
    onEditAlarm: (AlarmModel) -> Unit,
    onToggleAlarm: (AlarmModel, Boolean) -> Unit,
    onDeleteAlarm: (AlarmModel) -> Unit,
    onOpenSleep: () -> Unit
) {
    val next = remember(alarms, nowMillis / 1_000L) { findNextAlarm(alarms, nowMillis) }
    // Confirmación antes de eliminar: la alarma solo se borra si el usuario lo confirma.
    var pendingDelete by remember { mutableStateOf<AlarmModel?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("alarm_home"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TimeHero(nowMillis = nowMillis, next = next, animated = animated) }
        item {
            SleepSummaryCard(
                nowMillis = nowMillis,
                next = next,
                sleepPlan = sleepPlan,
                onOpenSleep = onOpenSleep
            )
        }
        item {
            SectionHeading(
                title = "Alarmas",
                detail = "${alarms.count { it.enabled }} activas"
            )
        }
        if (alarms.isEmpty()) {
            item { AlarmEmptyState() }
        } else {
            items(alarms, key = AlarmModel::id) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onEdit = { onEditAlarm(alarm) },
                    onToggle = { onToggleAlarm(alarm, it) },
                    onDelete = { pendingDelete = alarm },
                    modifier = if (LocalUiOptions.current.motionFactor > 0f) Modifier.animateItem() else Modifier
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
            containerColor = LocalDespiertaPalette.current.card.first(),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/** "7 h 30 min": cifras grandes y unidades pequeñas. */
private fun styledDuration(text: String, numberColor: Color, unitColor: Color): AnnotatedString =
    buildAnnotatedString {
        text.split(" ").forEachIndexed { index, part ->
            if (index > 0) append(" ")
            val isNumber = part.firstOrNull()?.isDigit() == true
            withStyle(
                SpanStyle(
                    fontSize = if (isNumber) 36.sp else 16.sp,
                    fontWeight = if (isNumber) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNumber) numberColor else unitColor
                )
            ) { append(part) }
        }
    }

@Composable
private fun SleepSummaryCard(
    nowMillis: Long,
    next: AlarmOccurrence?,
    sleepPlan: SleepPlan,
    onOpenSleep: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val remainingMillis = next?.triggerAtMillis?.minus(nowMillis)
    val sleepWindow = remainingMillis?.let(::formatSleepDuration)
    val guidance = when {
        remainingMillis == null -> "Activa una alarma para calcular tu descanso."
        remainingMillis < 7 * 60 * 60 * 1_000L ->
            "Descanso corto: prepara el ambiente antes de dormir."
        remainingMillis <= 9 * 60 * 60 * 1_000L ->
            "Dentro de la referencia de 7 a 9 horas para adultos."
        else -> "Aún tienes tiempo para una rutina tranquila."
    }
    val is24Hour = false // Formato de 12 horas en toda la app.

    GlowCard(
        modifier = Modifier.fillMaxWidth().testTag("sleep_duration_card"),
        corner = 24.dp,
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Descanso estimado", color = palette.textSoft, fontSize = 12.sp)
                if (sleepWindow == null) {
                    Text(
                        "Sin próxima alarma",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("sleep_duration_value")
                    )
                } else {
                    Text(
                        styledDuration(sleepWindow, palette.textPrimary, palette.textSoft),
                        modifier = Modifier.testTag("sleep_duration_value")
                    )
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, palette.accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            ) {
                IconButton(
                    onClick = onOpenSleep,
                    modifier = Modifier.fillMaxSize().testTag("open_sleep_planner")
                ) {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = "Configurar horas de sueño",
                        tint = palette.accentSoft
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(guidance, color = palette.textSoft, fontSize = 12.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable(onClick = onOpenSleep)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(DuskBlue))
            Spacer(Modifier.width(8.dp))
            Text(
                formatMinuteOfDay(sleepPlan.bedMinutes, is24Hour),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Text("  →  ", color = palette.textSoft, fontSize = 14.sp)
            Box(Modifier.size(9.dp).clip(CircleShape).background(DawnOrange))
            Spacer(Modifier.width(8.dp))
            Text(
                formatMinuteOfDay(sleepPlan.wakeMinutes, is24Hour),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
private fun TimeHero(nowMillis: Long, next: AlarmOccurrence?, animated: Boolean) {
    val palette = LocalDespiertaPalette.current
    // Segundero local: solo esta tarjeta se vuelve a dibujar cada segundo.
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
    val shownMillis = if (animated) secondMillis else nowMillis
    val now = Instant.ofEpochMilli(shownMillis).atZone(ZoneId.systemDefault())
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        brush = palette.heroBrush,
        corner = 30.dp,
        borderColor = palette.accent.copy(alpha = 0.22f)
    ) {
        // Cielo nocturno: luna con estrellas sobre las montañas.
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            MoonWithStars(
                modifier = Modifier
                    .size(116.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                animated = animated
            )
            NightLandscape(
                hill = palette.hill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter)
            )
        }
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 18.dp)) {
            Text(
                formatCivilDate(now.toLocalDate()),
                color = palette.accentSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.testTag("live_clock")
            ) {
                val (clockTime, clockPeriod) = clockParts(now.hour, now.minute)
                Text(
                    clockTime,
                    fontSize = 60.sp,
                    lineHeight = 62.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = palette.textPrimary
                )
                Column(Modifier.padding(start = 6.dp, bottom = 10.dp)) {
                    Text(
                        clockPeriod,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentSoft
                    )
                    Text(
                        now.format(DateTimeFormatter.ofPattern(":ss")),
                        fontSize = 14.sp,
                        color = palette.textSoft
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (next == null) Icons.Rounded.Schedule else Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(11.dp))
                if (next == null) {
                    Column(Modifier.weight(1f)) {
                        Text("Sin próximo despertar", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                        Text("Crea o activa una alarma", color = palette.textSoft, fontSize = 12.sp)
                    }
                } else {
                    Column(Modifier.weight(1f)) {
                        Text(
                            next.alarm.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text(formatOccurrence(next.triggerAtMillis), color = palette.textSoft, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "FALTAN",
                            color = palette.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            formatCountdown(next.triggerAtMillis - shownMillis),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = palette.textPrimary,
                            modifier = Modifier.testTag("next_alarm_countdown")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(detail, color = palette.textSoft, fontSize = 12.sp)
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmModel,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val active = alarm.enabled || alarm.snoozedUntilMillis != null
    val timeAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.55f,
        animationSpec = tween(motion(200)),
        label = "alarm_alpha"
    )
    GlowCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .animateContentSize(tween(motion(200))),
        brush = if (active) palette.activeBrush else null,
        corner = 22.dp,
        borderColor = if (active) palette.accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.06f)
    ) {
        Column(Modifier.padding(start = 18.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (alarmTime, alarmPeriod) = clockParts(alarm.hour, alarm.minute)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.alpha(timeAlpha).testTag("alarm_time_${alarm.id}")
                ) {
                    Text(
                        alarmTime,
                        fontSize = 38.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        alarmPeriod,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentSoft,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = despiertaSwitchColors(),
                    modifier = Modifier.testTag("alarm_toggle_${alarm.id}")
                )
            }
            Text(
                alarm.label,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (active) palette.textPrimary else palette.textSoft
            )
            Text(
                alarmScheduleLabel(alarm) + " · Repetir ${alarm.snoozeMinutes} min" +
                    if (alarm.mathChallengeEnabled) " · Desafío" else "",
                color = if (active) palette.accent else palette.textSoft,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            // Controles uniformes: misma forma y tamaño para editar y eliminar.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Editar",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).testTag("alarm_edit_${alarm.id}")
                )
                CardActionButton(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Eliminar",
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f).testTag("alarm_delete_${alarm.id}")
                )
            }
        }
    }
}

@Composable
private fun AlarmEmptyState() {
    val palette = LocalDespiertaPalette.current
    GlowCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AlarmAdd, contentDescription = null, tint = palette.accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Tu mañana empieza aquí", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.textPrimary)
                Text("Crea una alarma puntual, semanal o por turnos.", color = palette.textSoft, fontSize = 12.sp)
            }
        }
    }
}
