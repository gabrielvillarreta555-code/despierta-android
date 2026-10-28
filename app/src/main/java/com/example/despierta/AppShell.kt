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
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
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

// Estructura principal: secciones, cabecera y barra de pestañas.

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
    /** Hora fija (solo pruebas y capturas); se usa cuando el reloj vivo está desactivado. */
    fixedNowMillis: Long? = null,
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
    sleepSchedule: SleepSchedule = SleepSchedule(),
    onSleepScheduleChanged: (SleepSchedule) -> Unit = {},
    notificationPrefs: NotificationPrefs = NotificationPrefs(),
    onNotificationPrefsChanged: (NotificationPrefs) -> Unit = {},
    canPostNotifications: Boolean = true,
    defaultSnoozeMinutes: Int = SNOOZE_OPTIONS.first(),
    onDefaultSnoozeChanged: (Int) -> Unit = {},
    onCreateWakeAlarm: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    themeChoice: ThemeChoice = ThemeChoice.DEFAULT,
    onThemeChanged: (ThemeChoice) -> Unit = {}
) {
    val palette = LocalDespiertaPalette.current
    var nowMillis by remember { mutableLongStateOf(fixedNowMillis ?: System.currentTimeMillis()) }
    DisposableEffect(liveClockUpdates) {
        if (!liveClockUpdates) {
            nowMillis = fixedNowMillis ?: System.currentTimeMillis()
            return@DisposableEffect onDispose { }
        }
        val handler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                // Las pantallas solo necesitan la hora al minuto: así no se recalcula todo cada
                // segundo. La tarjeta del reloj lleva su propio segundero (ClockCard).
                nowMillis = System.currentTimeMillis()
                val nextTick = (60_000L - nowMillis % 60_000L).coerceAtLeast(250L)
                handler.postDelayed(this, nextTick)
            }
        }
        handler.post(ticker)
        onDispose { handler.removeCallbacks(ticker) }
    }
    val enterMillis = motion(220)
    val exitMillis = motion(120)

    Box(Modifier.fillMaxSize().background(palette.skyBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // En Alarmas la cabecera forma parte del paisaje (AlarmHome); el resto de pantallas
            // mantiene la cabecera común hasta su rediseño en la 2.7.
            topBar = {
                if (section != AppSection.ALARMS) BrandBar()
            },
            bottomBar = {
                AppNavigation(
                    selected = section,
                    permissionCount = permissionState.pendingCount,
                    onSelected = onSectionSelected
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    (fadeIn(tween(enterMillis)) + slideInVertically(tween(enterMillis)) { it / 30 })
                        .togetherWith(fadeOut(tween(exitMillis)))
                },
                label = "section",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { selected ->
                when (selected) {
                    AppSection.ALARMS -> AlarmHome(
                        alarms = alarms,
                        nowMillis = nowMillis,
                        sleepPlan = sleepSchedule.plan,
                        animated = liveClockUpdates,
                        onAddAlarm = onAddAlarm,
                        onEditAlarm = onEditAlarm,
                        onToggleAlarm = onToggleAlarm,
                        onDeleteAlarm = onDeleteAlarm,
                        onOpenSleep = { onSectionSelected(AppSection.SLEEP) }
                    )

                    AppSection.SLEEP -> SleepScreen(
                        schedule = sleepSchedule,
                        notifications = notificationPrefs,
                        canPostNotifications = canPostNotifications,
                        alarms = alarms,
                        nowMillis = nowMillis,
                        hapticsEnabled = hapticsEnabled,
                        onScheduleChanged = onSleepScheduleChanged,
                        onNotificationsChanged = onNotificationPrefsChanged,
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
                        onThemeChanged = onThemeChanged,
                        sleepSchedule = sleepSchedule,
                        onSleepScheduleChanged = onSleepScheduleChanged,
                        notificationPrefs = notificationPrefs,
                        onNotificationPrefsChanged = onNotificationPrefsChanged,
                        canPostNotifications = canPostNotifications,
                        defaultSnoozeMinutes = defaultSnoozeMinutes,
                        onDefaultSnoozeChanged = onDefaultSnoozeChanged,
                        onOpenSleep = { onSectionSelected(AppSection.SLEEP) }
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
            .padding(top = safeTopInset())
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "DESPIERTA",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = palette.textPrimary
            )
            Text("Tu tiempo, bajo control", color = palette.textSoft, fontSize = 12.sp)
        }
        if (onAddAlarm != null) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.button)
                    .clickable(onClick = onAddAlarm)
                    .semantics { contentDescription = "Nueva alarma" }
                    .testTag("add_alarm"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.onButton)
            }
        }
    }
}

/**
 * Barra de pestañas (2.6): una píldora con los cuatro accesos. La pestaña activa se abre en una
 * cápsula con su nombre y el color de navegación elegido; las demás muestran solo el icono.
 */
@Composable
private fun AppNavigation(
    selected: AppSection,
    permissionCount: Int,
    onSelected: (AppSection) -> Unit
) {
    val palette = LocalDespiertaPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppSection.entries.forEach { section ->
            val isSelected = selected == section
            val background by animateColorAsState(
                targetValue = if (isSelected) palette.navIconSelected else Color.Transparent,
                animationSpec = tween(motion(200)),
                label = "nav_background"
            )
            val tint = if (isSelected) contrastOn(palette.navIconSelected) else palette.navIcon
            Row(
                Modifier
                    .height(52.dp)
                    .animateContentSize(tween(motion(200)))
                    .clip(CircleShape)
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelected(section) },
                        role = Role.Tab
                    )
                    .semantics { contentDescription = section.label }
                    .testTag("nav_${section.name.lowercase()}")
                    .padding(horizontal = if (isSelected) 18.dp else 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Icon(section.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
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
                    Text(
                        section.label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
