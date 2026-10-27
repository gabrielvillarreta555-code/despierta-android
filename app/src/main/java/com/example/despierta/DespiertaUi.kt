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

internal enum class AppSection(val label: String, val icon: ImageVector) {
    ALARMS("Alarmas", Icons.Rounded.Alarm),
    SLEEP("Sueño", Icons.Rounded.Bedtime),
    SHIFTS("Turnos", Icons.Rounded.CalendarMonth),
    SETTINGS("Ajustes", Icons.Rounded.Settings)
}

internal data class PermissionUiState(
    val needsExactAlarm: Boolean,
    val needsNotifications: Boolean,
    val needsFullScreen: Boolean,
    val needsAlarmChannel: Boolean
) {
    val pendingCount: Int
        get() = listOf(
            needsExactAlarm,
            needsNotifications,
            needsFullScreen,
            needsAlarmChannel
        ).count { it }
}

@Composable
internal fun DespiertaApp(
    alarms: List<AlarmModel>,
    section: AppSection,
    permissionState: PermissionUiState,
    hapticsEnabled: Boolean,
    liveClockUpdates: Boolean = true,
    snackbarHostState: SnackbarHostState,
    onSectionSelected: (AppSection) -> Unit,
    onAddAlarm: () -> Unit,
    onAddShiftAlarm: () -> Unit,
    onEditAlarm: (AlarmModel) -> Unit,
    onToggleAlarm: (AlarmModel, Boolean) -> Unit,
    onDeleteAlarm: (AlarmModel) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onRequestAlarmChannel: () -> Unit,
    sleepPlan: SleepPlan = SleepPlan.DEFAULT,
    onSleepPlanChanged: (SleepPlan) -> Unit = {},
    onCreateWakeAlarm: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    themeChoice: ThemeChoice = ThemeChoice(DespiertaPresets.first().id, null),
    onThemeChanged: (ThemeChoice) -> Unit = {}
) {
    val palette = LocalDespiertaPalette.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(liveClockUpdates) {
        if (!liveClockUpdates) {
            nowMillis = System.currentTimeMillis()
            return@DisposableEffect onDispose { }
        }
        val handler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                nowMillis = System.currentTimeMillis()
                val nextTick = (1_000L - nowMillis % 1_000L).coerceAtLeast(100L)
                handler.postDelayed(this, nextTick)
            }
        }
        handler.post(ticker)
        onDispose { handler.removeCallbacks(ticker) }
    }

    Box(Modifier.fillMaxSize().background(palette.skyBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BrandBar(onAddAlarm = if (section == AppSection.ALARMS) onAddAlarm else null)
            },
            bottomBar = {
                AppNavigation(
                    selected = section,
                    permissionCount = permissionState.pendingCount,
                    onSelected = onSectionSelected
                )
            }
        ) { innerPadding ->
            Crossfade(
                targetState = section,
                animationSpec = tween(180),
                label = "section",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { selected ->
                when (selected) {
                    AppSection.ALARMS -> AlarmHome(
                        alarms = alarms,
                        nowMillis = nowMillis,
                        sleepPlan = sleepPlan,
                        animated = liveClockUpdates,
                        onAddAlarm = onAddAlarm,
                        onEditAlarm = onEditAlarm,
                        onToggleAlarm = onToggleAlarm,
                        onDeleteAlarm = onDeleteAlarm,
                        onOpenSleep = { onSectionSelected(AppSection.SLEEP) }
                    )

                    AppSection.SLEEP -> SleepScreen(
                        plan = sleepPlan,
                        alarms = alarms,
                        nowMillis = nowMillis,
                        hapticsEnabled = hapticsEnabled,
                        onPlanChanged = onSleepPlanChanged,
                        onCreateWakeAlarm = onCreateWakeAlarm,
                        onBack = { onSectionSelected(AppSection.ALARMS) }
                    )

                    AppSection.SHIFTS -> ShiftScreen(
                        alarms = alarms,
                        nowMillis = nowMillis,
                        onAddShiftAlarm = onAddShiftAlarm,
                        onEditAlarm = onEditAlarm,
                        onToggleAlarm = onToggleAlarm
                    )

                    AppSection.SETTINGS -> SettingsScreen(
                        permissionState = permissionState,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChanged = onHapticsChanged,
                        onRequestExactAlarm = onRequestExactAlarm,
                        onRequestNotifications = onRequestNotifications,
                        onRequestFullScreen = onRequestFullScreen,
                        onRequestAlarmChannel = onRequestAlarmChannel,
                        themeChoice = themeChoice,
                        onThemeChanged = onThemeChanged
                    )
                }
            }
        }
    }
}

/** Cabecera fija: en Alarmas incluye la acción "Nueva alarma", siempre visible. */
@Composable
private fun BrandBar(onAddAlarm: (() -> Unit)? = null) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "DESPIERTA",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.4.sp,
            color = palette.textPrimary
        )
        Spacer(Modifier.weight(1f))
        if (onAddAlarm != null) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accentBrush)
                    .clickable(onClick = onAddAlarm)
                    .semantics { contentDescription = "Nueva alarma" }
                    .testTag("add_alarm"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.onAccent)
            }
        } else {
            Text("Tu tiempo, bajo control", color = palette.textSoft, fontSize = 11.sp)
        }
    }
}

/** Barra inferior en forma de píldora; la pestaña activa muestra su nombre. */
@Composable
private fun AppNavigation(
    selected: AppSection,
    permissionCount: Int,
    onSelected: (AppSection) -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.activeBrush)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSection.entries.forEach { section ->
                val isSelected = selected == section
                Row(
                    Modifier
                        .weight(if (isSelected) 1.7f else 1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .then(if (isSelected) Modifier.background(palette.accentBrush) else Modifier)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelected(section) },
                            role = Role.Tab
                        )
                        .semantics { contentDescription = section.label }
                        .testTag("nav_${section.name.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            tint = if (isSelected) palette.onAccent else palette.textSoft,
                            modifier = Modifier.size(22.dp)
                        )
                        if (section == AppSection.SETTINGS && permissionCount > 0) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                    if (isSelected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            section.label,
                            color = palette.onAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Botón principal sencillo, dentro del contenido para que nunca tape nada. */
@Composable
private fun AddButton(label: String, tag: String, onClick: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlarmHome(
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
    val next = remember(alarms, nowMillis / 1_000L) { findNextAlarm(alarms, nowMillis) }
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
                    onClick = { onEditAlarm(alarm) },
                    onToggle = { onToggleAlarm(alarm, it) },
                    onDelete = { onDeleteAlarm(alarm) }
                )
            }
        }
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
    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
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
                            formatCountdown(next.triggerAtMillis - nowMillis),
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
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    val active = alarm.enabled || alarm.snoozedUntilMillis != null
    GlowCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).animateContentSize(),
        brush = if (active) palette.activeBrush else null,
        corner = 22.dp,
        borderColor = if (active) palette.accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.06f)
    ) {
        Column(Modifier.padding(start = 18.dp, top = 12.dp, end = 10.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (alarmTime, alarmPeriod) = clockParts(alarm.hour, alarm.minute)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.testTag("alarm_time_${alarm.id}")
                ) {
                    Text(
                        alarmTime,
                        fontSize = 38.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) palette.textPrimary else palette.textSoft
                    )
                    Text(
                        alarmPeriod,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) palette.accentSoft else palette.textSoft,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        alarm.label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (active) palette.textPrimary else palette.textSoft
                    )
                    Text(
                        alarmScheduleLabel(alarm),
                        color = if (active) palette.accent else palette.textSoft,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (alarm.mathChallengeEnabled) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.18f))
                    ) {
                        Text(
                            "DESAFÍO",
                            color = palette.accentSoft,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar ${alarm.label}",
                        tint = palette.textSoft
                    )
                }
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

@Composable
private fun ShiftScreen(
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

@Composable
private fun SettingsScreen(
    permissionState: PermissionUiState,
    hapticsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onRequestAlarmChannel: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChanged: (ThemeChoice) -> Unit
) {
    val palette = LocalDespiertaPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text("Personaliza Despierta", color = palette.textSoft, fontSize = 13.sp)
            }
        }
        item {
            SettingsSection(title = "APARIENCIA") {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Fondo", color = palette.textSoft, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DespiertaPresets.forEach { preset ->
                            PresetTile(
                                preset = preset,
                                selected = preset.id == themeChoice.presetId,
                                modifier = Modifier.weight(1f),
                                onClick = { onThemeChanged(ThemeChoice(preset.id, null)) }
                            )
                        }
                    }
                    Text("Color de acento", color = palette.textSoft, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DespiertaAccents.forEach { (name, color) ->
                            AccentSwatch(
                                name = name,
                                color = color,
                                selected = color == palette.accent,
                                onClick = { onThemeChanged(themeChoice.copy(accent = color)) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(title = "ALARMAS Y RESPUESTA") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Vibration, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Respuesta háptica", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                        Text("Vibración breve al usar los controles", color = palette.textSoft, fontSize = 12.sp)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsChanged,
                        colors = despiertaSwitchColors()
                    )
                }
                HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                SettingsStatusRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Sonido bajo tu control",
                    detail = "Despierta no cambia el volumen ni ignora No molestar."
                )
            }
        }
        item {
            SettingsSection(title = "FIABILIDAD") {
                if (permissionState.pendingCount == 0) {
                    SettingsStatusRow(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Todo listo",
                        detail = "Android permite notificar y mostrar tus alarmas.",
                        accent = true
                    )
                } else {
                    if (permissionState.needsExactAlarm) {
                        PermissionRow(
                            title = "Alarmas exactas",
                            detail = "Permite registrar la hora exacta incluso durante Doze",
                            onClick = onRequestExactAlarm
                        )
                        HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                    }
                    if (permissionState.needsNotifications) {
                        PermissionRow(
                            title = "Notificaciones",
                            detail = "Actívalas para ver Detener y Posponer mientras suena",
                            onClick = onRequestNotifications
                        )
                        HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                    }
                    if (permissionState.needsFullScreen) {
                        PermissionRow(
                            title = "Pantalla completa",
                            detail = "Autoriza mostrar la alarma sobre la pantalla bloqueada",
                            onClick = onRequestFullScreen
                        )
                        HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                    }
                    if (permissionState.needsAlarmChannel) {
                        PermissionRow(
                            title = "Canal sin prioridad alta",
                            detail = "Restaura la prioridad de Alarmas activas en Android",
                            onClick = onRequestAlarmChannel
                        )
                        HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                    }
                }
                SettingsStatusRow(
                    icon = Icons.Rounded.Shield,
                    title = "Recuperación automática",
                    detail = "Se reprograman tras reiniciar, actualizar o cambiar la hora. Algunos fabricantes requieren permitir actividad en segundo plano."
                )
            }
        }
        item {
            SettingsSection(title = "ACERCA DE") {
                Row(
                    Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(palette.accentBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = palette.onAccent)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Despierta ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(
                            "Compilación ${BuildConfig.VERSION_CODE} · ${BuildConfig.GIT_COMMIT}",
                            color = palette.textSoft,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = palette.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        GlowCard(modifier = Modifier.fillMaxWidth(), corner = 20.dp) {
            content()
        }
    }
}

@Composable
private fun PresetTile(
    preset: DespiertaPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDespiertaPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(preset.skyBrush)
            .border(
                BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) palette.accent else Color.White.copy(alpha = 0.12f)
                ),
                shape
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag("theme_${preset.id}")
    ) {
        NightLandscape(hill = preset.hill, modifier = Modifier.fillMaxWidth().height(52.dp))
        Text(
            preset.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AccentSwatch(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(BorderStroke(if (selected) 3.dp else 0.dp, Color.White), CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = "Color $name" }
            .testTag("accent_$name")
    )
}

@Composable
private fun PermissionRow(title: String, detail: String, onClick: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(detail, color = palette.textSoft, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
    }
}

@Composable
private fun SettingsStatusRow(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Boolean = false
) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (accent) palette.accent else palette.textSoft)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(detail, color = palette.textSoft, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AlarmEditor(
    initial: AlarmModel,
    isNew: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmModel) -> Unit
) {
    val validInitialShift = initial.shiftPattern?.takeIf { it.isValid }
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var days by remember(initial.id) { mutableStateOf(initial.days) }
    var soundUri by remember(initial.id) { mutableStateOf(initial.soundUri) }
    var vibrate by remember(initial.id) { mutableStateOf(initial.vibrate) }
    var snoozeMinutes by remember(initial.id) {
        mutableIntStateOf(AlarmStore.normalizeSnoozeMinutes(initial.snoozeMinutes))
    }
    var mathChallenge by remember(initial.id) { mutableStateOf(initial.mathChallengeEnabled) }
    var useShift by remember(initial.id) { mutableStateOf(initial.shiftPattern != null) }
    var workDays by remember(initial.id) { mutableStateOf(validInitialShift?.workDays?.toString() ?: "12") }
    var restDays by remember(initial.id) { mutableStateOf(validInitialShift?.restDays?.toString() ?: "4") }
    var startEpochDay by remember(initial.id) {
        mutableLongStateOf(validInitialShift?.startEpochDay ?: LocalDate.now().toEpochDay())
    }
    val context = LocalContext.current
    // Toda la app usa formato de 12 horas con AM/PM, sin depender del ajuste del teléfono.
    val uses24HourClock = false
    var selectedHour by remember(initial.id) { mutableIntStateOf(initial.hour) }
    var selectedMinute by remember(initial.id) { mutableIntStateOf(initial.minute) }
    var manualEntryVisible by remember(initial.id) { mutableStateOf(false) }
    var manualTime by remember(initial.id, uses24HourClock) {
        mutableStateOf(formatManualTime(initial.hour, initial.minute, uses24HourClock))
    }

    val shiftPatternCandidate = if (useShift) {
        val work = workDays.toIntOrNull()
        val rest = restDays.toIntOrNull()
        if (work != null && rest != null) {
            ShiftPattern(
                startEpochDay = startEpochDay,
                workDays = work,
                restDays = rest,
                exceptionEpochDays = validInitialShift?.exceptionEpochDays ?: emptySet()
            )
        } else {
            null
        }
    } else {
        null
    }
    val shiftInputValid = !useShift || shiftPatternCandidate?.isValid == true
    val manualTimeValid = !manualEntryVisible || parseManualTime(manualTime, uses24HourClock) != null
    val tonePreview = remember(initial.id) { AlarmTonePreview(context) }
    var tonePreviewState by remember(initial.id) { mutableStateOf(TonePreviewState.IDLE) }
    DisposableEffect(tonePreview) {
        onDispose { tonePreview.stop() }
    }
    // El título se resuelve fuera del hilo principal y sin crear un Ringtone: getRingtone()
    // instancia un MediaPlayer que nunca se liberaba y bloqueaba la apertura del editor.
    val toneTitle by produceState(initialValue = "Tono predeterminado", soundUri) {
        value = withContext(Dispatchers.IO) { resolveToneTitle(context, soundUri) }
            ?: "Tono predeterminado"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
            tonePreview.stop()
            soundUri = uri.toString()
        }
    }

    fun save() {
        val pattern = shiftPatternCandidate
        if (useShift && pattern == null) return
        onSave(
            initial.copy(
                hour = selectedHour,
                minute = selectedMinute,
                label = label.trim().ifBlank { "Alarma" },
                days = if (useShift) emptySet() else days,
                shiftPattern = pattern,
                soundUri = soundUri.ifBlank { AlarmStore.defaultAlarmUri() },
                vibrate = vibrate,
                snoozeMinutes = snoozeMinutes,
                enabled = true,
                mathChallengeEnabled = mathChallenge,
                snoozedUntilMillis = null
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(LocalDespiertaPalette.current.skyBrush),
            color = Color.Transparent
        ) {
            Column(Modifier.safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (isNew) "Nueva alarma" else "Editar alarma", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Hora, repetición y sonido", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Button(onClick = ::save, enabled = shiftInputValid && manualTimeValid) {
                        Text("Guardar")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditorSection("HORA") {
                        TimeWheelPicker(
                            hour24 = selectedHour,
                            minute = selectedMinute,
                            is24Hour = uses24HourClock,
                            hapticsEnabled = hapticsEnabled,
                            onTimeChanged = { hour, minute ->
                                selectedHour = hour
                                selectedMinute = minute
                                manualTime = formatManualTime(hour, minute, uses24HourClock)
                            }
                        )
                        TextButton(
                            onClick = {
                                manualEntryVisible = !manualEntryVisible
                                manualTime = formatManualTime(
                                    selectedHour,
                                    selectedMinute,
                                    uses24HourClock
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (manualEntryVisible) "Ocultar entrada manual" else "Introducir hora manualmente")
                        }
                        if (manualEntryVisible) {
                            OutlinedTextField(
                                value = manualTime,
                                onValueChange = { entered ->
                                    manualTime = entered.take(11)
                                    parseManualTime(entered, uses24HourClock)?.let { parsed ->
                                        selectedHour = parsed.hour24
                                        selectedMinute = parsed.minute
                                    }
                                },
                                label = {
                                    Text(if (uses24HourClock) "Hora (HH:MM)" else "Hora (HH:MM AM/PM)")
                                },
                                isError = !manualTimeValid,
                                supportingText = {
                                    if (!manualTimeValid) {
                                        Text(
                                            if (uses24HourClock) {
                                                "Usa una hora entre 00:00 y 23:59"
                                            } else {
                                                "Usa una hora como 07:30 AM o 07:30 PM"
                                            }
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.fillMaxWidth().testTag("manual_time_input")
                            )
                        }
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it.take(60) },
                            label = { Text("Nombre de la alarma") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    EditorSection("PROGRAMACIÓN") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Turno inteligente", fontWeight = FontWeight.SemiBold)
                                Text("Alterna automáticamente trabajo y descanso", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(checked = useShift, onCheckedChange = { useShift = it }, colors = despiertaSwitchColors())
                        }
                        if (useShift) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = workDays,
                                    onValueChange = { workDays = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Trabajo") },
                                    suffix = { Text("días") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = (workDays.toIntOrNull() ?: 0) < 1,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = restDays,
                                    onValueChange = { restDays = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Descanso") },
                                    suffix = { Text("días") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = (restDays.toIntOrNull() ?: 0) < 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (!shiftInputValid) {
                                Text(
                                    "Usa al menos un día de trabajo y uno de descanso; el ciclo admite hasta 366 días.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                            Text("Inicio del ciclo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { startEpochDay-- },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Día anterior")
                                }
                                OutlinedButton(
                                    onClick = { startEpochDay = LocalDate.now().toEpochDay() },
                                    modifier = Modifier.widthIn(min = 180.dp)
                                ) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatCivilDate(LocalDate.ofEpochDay(startEpochDay)))
                                }
                                IconButton(
                                    onClick = { startEpochDay++ },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Día siguiente")
                                }
                            }
                            Text(
                                "Toca la fecha para volver a hoy; usa las flechas para sincronizar el primer día de trabajo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            Text("Repetir", fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                listOf("D", "L", "M", "X", "J", "V", "S").forEachIndexed { index, text ->
                                    val day = index + 1
                                    FilterChip(
                                        selected = day in days,
                                        onClick = { days = if (day in days) days - day else days + day },
                                        label = { Text(text) }
                                    )
                                }
                            }
                            Text(
                                if (days.isEmpty()) "Sonará una sola vez" else daysLabel(days),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    EditorSection("SONIDO Y CONTROL") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Desafío matemático", fontWeight = FontWeight.SemiBold)
                                Text("Será obligatorio para detener", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(checked = mathChallenge, onCheckedChange = { mathChallenge = it }, colors = despiertaSwitchColors())
                        }
                        Text("Posponer", fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SUPPORTED_SNOOZE_MINUTES.sorted().forEach { minutes ->
                                FilterChip(
                                    selected = snoozeMinutes == minutes,
                                    onClick = { snoozeMinutes = minutes },
                                    label = { Text("$minutes min") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("snooze_$minutes")
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Vibration, contentDescription = null, tint = LocalDespiertaPalette.current.accent)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Vibración", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (vibrate) "Vibrará mientras suena" else "Desactivada para esta alarma",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = vibrate,
                                onCheckedChange = { vibrate = it },
                                colors = despiertaSwitchColors(),
                                modifier = Modifier.testTag("alarm_vibration")
                            )
                        }
                        Text("Tono de alarma", fontWeight = FontWeight.SemiBold)
                        Text(
                            toneTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("selected_tone")
                        )
                        OutlinedButton(
                            onClick = {
                                picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                                })
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elegir sonido")
                        }
                        OutlinedButton(
                            onClick = {
                                if (
                                    tonePreviewState == TonePreviewState.PREPARING ||
                                    tonePreviewState == TonePreviewState.PLAYING ||
                                    tonePreviewState == TonePreviewState.PLAYING_FALLBACK
                                ) {
                                    tonePreview.stop()
                                } else {
                                    tonePreview.play(soundUri) { tonePreviewState = it }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("preview_tone")
                        ) {
                            val previewActive = tonePreviewState == TonePreviewState.PREPARING ||
                                tonePreviewState == TonePreviewState.PLAYING ||
                                tonePreviewState == TonePreviewState.PLAYING_FALLBACK
                            Icon(
                                if (previewActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (previewActive) "Detener preescucha" else "Preescuchar tono")
                        }
                        when (tonePreviewState) {
                            TonePreviewState.PLAYING_FALLBACK -> Text(
                                "El tono elegido no está disponible; se escucha el predeterminado.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                            TonePreviewState.FAILED -> Text(
                                "No se pudo reproducir el tono. Android usará un sonido predeterminado al sonar.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                            else -> Unit
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(
            title,
            color = LocalDespiertaPalette.current.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = { content() }
            )
        }
    }
}

/** Lee el título del tono desde su proveedor de contenido, sin reproducir ni retener audio. */
private fun resolveToneTitle(context: android.content.Context, soundUri: String): String? = runCatching {
    var uri = Uri.parse(soundUri.ifBlank { return@runCatching null })
    if (uri.authority == android.provider.Settings.AUTHORITY) {
        uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: return@runCatching null
    }
    context.contentResolver.query(
        uri,
        arrayOf(android.provider.MediaStore.MediaColumns.TITLE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0)?.takeIf(String::isNotBlank) else null
    }
}.getOrNull()

private fun alarmScheduleLabel(alarm: AlarmModel): String {
    val schedule = alarm.shiftPattern?.let { "Turno ${it.workDays}×${it.restDays}" } ?: daysLabel(alarm.days)
    return if (alarm.snoozedUntilMillis != null) "Pospuesta · $schedule" else schedule
}

internal fun daysLabel(days: Set<Int>): String = when {
    days.isEmpty() -> "Una vez"
    days.size == 7 -> "Todos los días"
    days == setOf(2, 3, 4, 5, 6) -> "Entre semana"
    else -> listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        .filterIndexed { index, _ -> index + 1 in days }
        .joinToString(" · ")
}

private fun formatCivilDate(date: LocalDate): String = date
    .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SPANISH))
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH) else it.toString() }

internal fun formatOccurrence(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    return at.format(DateTimeFormatter.ofPattern("EEE d MMM", SPANISH)) +
        " · " + formatClockTime(at.hour, at.minute)
}

internal fun formatCompactOccurrence(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    return at.format(DateTimeFormatter.ofPattern("EEE d", SPANISH)) +
        " · " + formatClockTime(at.hour, at.minute)
}

internal fun formatCountdown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
    if (totalSeconds == 0L) return "Ahora"
    val days = totalSeconds / 86_400L
    val hours = totalSeconds % 86_400L / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (days > 0L) {
        "%d d · %02d:%02d:%02d".format(days, hours, minutes, seconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

internal fun formatSleepDuration(remainingMillis: Long): String {
    val totalMinutes = (remainingMillis.coerceAtLeast(0L) + 59_999L) / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = totalMinutes % (24L * 60L) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L -> "%d d · %d h %02d min".format(days, hours, minutes)
        hours > 0L -> "%d h %02d min".format(hours, minutes)
        minutes > 0L -> "%d min".format(minutes)
        else -> "Ahora"
    }
}

private val SPANISH = Locale("es", "ES")
