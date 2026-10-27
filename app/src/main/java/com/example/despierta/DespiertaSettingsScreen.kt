package com.example.despierta

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class SettingsPage(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Apariencia", "Fondo, colores e iconos", Icons.Rounded.Palette),
    ALARMS("Alarmas", "Repetición y formato de hora", Icons.Rounded.Alarm),
    SOUNDS("Sonidos y vibración", "Respuesta háptica y volumen", Icons.Rounded.MusicNote),
    NOTIFICATIONS("Notificaciones", "Recordatorios y avisos opcionales", Icons.Rounded.NotificationsActive),
    SLEEP("Sueño", "Objetivo y recordatorio", Icons.Rounded.Bedtime),
    ACCESSIBILITY("Accesibilidad", "Tamaño del texto y animaciones", Icons.Rounded.FormatSize),
    RELIABILITY("Fiabilidad y permisos", "Que tus alarmas suenen siempre", Icons.Rounded.Shield),
    ABOUT("Información de Despierta", "Versión y privacidad", Icons.Rounded.Info)
}

@Composable
internal fun SettingsScreen(
    permissionState: PermissionUiState,
    hapticsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onRequestAlarmChannel: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChanged: (ThemeChoice) -> Unit,
    sleepSchedule: SleepSchedule,
    onSleepScheduleChanged: (SleepSchedule) -> Unit,
    notificationPrefs: NotificationPrefs,
    onNotificationPrefsChanged: (NotificationPrefs) -> Unit,
    canPostNotifications: Boolean,
    defaultSnoozeMinutes: Int,
    onDefaultSnoozeChanged: (Int) -> Unit,
    onOpenSleep: () -> Unit
) {
    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    val page = pageName?.let { name -> SettingsPage.entries.firstOrNull { it.name == name } }
    BackHandler(enabled = page != null) { pageName = null }
    val enter = motion(220)
    val exit = motion(120)

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(tween(enter)) + slideInHorizontally(tween(enter)) { if (targetState != null) it / 12 else -it / 12 })
                .togetherWith(fadeOut(tween(exit)))
        },
        label = "settings_page",
        modifier = Modifier.fillMaxSize().testTag("settings_screen")
    ) { current ->
        if (current == null) {
            SettingsHome(
                pendingPermissions = permissionState.pendingCount,
                onOpen = { pageName = it.name }
            )
        } else {
            SettingsDetail(title = current.title, onBack = { pageName = null }, tag = "settings_page_${current.name.lowercase()}") {
                when (current) {
                    SettingsPage.APPEARANCE -> AppearanceSettings(themeChoice, onThemeChanged)
                    SettingsPage.ALARMS -> AlarmSettings(defaultSnoozeMinutes, onDefaultSnoozeChanged)
                    SettingsPage.SOUNDS -> SoundSettings(hapticsEnabled, onHapticsChanged, onOpenNotifications = {
                        pageName = SettingsPage.NOTIFICATIONS.name
                    })
                    SettingsPage.NOTIFICATIONS -> NotificationSettings(
                        prefs = notificationPrefs,
                        canPost = canPostNotifications,
                        onChanged = onNotificationPrefsChanged,
                        onRequestNotifications = onRequestNotifications
                    )
                    SettingsPage.SLEEP -> SleepSettings(
                        schedule = sleepSchedule,
                        onScheduleChanged = onSleepScheduleChanged,
                        notifications = notificationPrefs,
                        onNotificationsChanged = onNotificationPrefsChanged,
                        onOpenSleep = onOpenSleep
                    )
                    SettingsPage.ACCESSIBILITY -> AccessibilitySettings(themeChoice, onThemeChanged)
                    SettingsPage.RELIABILITY -> ReliabilitySettings(
                        permissionState = permissionState,
                        onRequestExactAlarm = onRequestExactAlarm,
                        onRequestNotifications = onRequestNotifications,
                        onRequestFullScreen = onRequestFullScreen,
                        onRequestAlarmChannel = onRequestAlarmChannel
                    )
                    SettingsPage.ABOUT -> AboutSettings()
                }
            }
        }
    }
}

@Composable
private fun SettingsHome(pendingPermissions: Int, onOpen: (SettingsPage) -> Unit) {
    val palette = LocalDespiertaPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Ajustes", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text("Personaliza Despierta", color = palette.textSoft, fontSize = 13.sp)
        }
        if (pendingPermissions > 0) {
            GlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(SettingsPage.RELIABILITY) }
                    .testTag("settings_permission_banner"),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                contentPadding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StyledIcon(Icons.Rounded.WarningAmber, tint = MaterialTheme.colorScheme.error)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            if (pendingPermissions == 1) "1 permiso pendiente" else "$pendingPermissions permisos pendientes",
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text("Revísalo para que tus alarmas suenen a tiempo.", fontSize = 12.sp, color = palette.textSoft)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
                }
            }
        }
        val groups = listOf(
            listOf(SettingsPage.APPEARANCE, SettingsPage.ACCESSIBILITY),
            listOf(SettingsPage.ALARMS, SettingsPage.SOUNDS, SettingsPage.NOTIFICATIONS, SettingsPage.SLEEP),
            listOf(SettingsPage.RELIABILITY, SettingsPage.ABOUT)
        )
        groups.forEach { group ->
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                group.forEachIndexed { index, item ->
                    if (index > 0) RowDivider()
                    NavigationRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { onOpen(item) },
                        modifier = Modifier.testTag("settings_${item.name.lowercase()}")
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SettingsDetail(
    title: String,
    onBack: () -> Unit,
    tag: String,
    content: @Composable () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("settings_back")
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Volver a Ajustes", tint = palette.textPrimary)
            }
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        content()
        Spacer(Modifier.height(4.dp))
    }
}

// ---------- Apariencia ----------

@Composable
private fun AppearanceSettings(choice: ThemeChoice, onChanged: (ThemeChoice) -> Unit) {
    val palette = LocalDespiertaPalette.current
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    SettingsGroup("FONDO") {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DespiertaPresets.forEach { preset ->
                PresetTile(
                    preset = preset,
                    selected = preset.id == choice.presetId,
                    modifier = Modifier.weight(1f),
                    onClick = { onChanged(choice.copy(presetId = preset.id)) }
                )
            }
        }
    }

    SettingsGroup("COLORES") {
        ColorChoiceRow("Acento general", choice.accent, "accent") { onChanged(choice.copy(accent = it)) }
        RowDivider()
        ColorChoiceRow("Iconos de navegación", choice.navIconColor, "nav_icon") { onChanged(choice.copy(navIconColor = it)) }
        RowDivider()
        ColorChoiceRow("Iconos de las tarjetas", choice.cardIconColor, "card_icon") { onChanged(choice.copy(cardIconColor = it)) }
        RowDivider()
        ColorChoiceRow("Botones", choice.buttonColor, "button") { onChanged(choice.copy(buttonColor = it)) }
    }
    FootNote("«Auto» usa el color del fondo elegido. El texto sobre cada color se ajusta solo para mantenerse legible.")

    SettingsGroup("ESTILO DE ICONOS") {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconStyle.entries.forEach { style ->
                val selected = style == choice.iconStyle
                val border by animateColorAsState(
                    if (selected) palette.accent else Color.White.copy(alpha = 0.08f),
                    tween(motion(180)),
                    label = "icon_style_border"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(BorderStroke(if (selected) 2.dp else 1.dp, border), RoundedCornerShape(16.dp))
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            onChanged(choice.copy(iconStyle = style))
                        })
                        .padding(vertical = 12.dp)
                        .testTag("icon_style_${style.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Vista previa del estilo, sin depender del estilo actual.
                    CompositionLocalProvider(
                        LocalUiOptions provides LocalUiOptions.current.copy(iconStyle = style)
                    ) {
                        StyledIcon(Icons.Rounded.Alarm)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(style.label, fontSize = 12.sp, color = palette.textPrimary)
                }
            }
        }
    }

    SettingsGroup("RESTAURAR") {
        ActionRow(
            icon = Icons.Rounded.RestartAlt,
            title = "Restaurar apariencia predeterminada",
            subtitle = "Fondo Noche, colores automáticos, iconos redondeados, texto normal y animaciones discretas",
            onClick = { confirmReset = true },
            modifier = Modifier.testTag("appearance_reset")
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Restaurar la apariencia?") },
            text = { Text("Se recuperan el fondo, los colores, los iconos, el tamaño del texto y las animaciones originales. Tus alarmas no cambian.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onChanged(ThemeChoice.DEFAULT)
                }, modifier = Modifier.testTag("appearance_reset_confirm")) {
                    Text("Restaurar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
            containerColor = palette.card.first(),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ColorChoiceRow(title: String, current: Color?, tagPrefix: String, onPick: (Color?) -> Unit) {
    val palette = LocalDespiertaPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(name = "Auto", color = null, selected = current == null, tag = "${tagPrefix}_auto") { onPick(null) }
            DespiertaAccents.forEach { (name, color) ->
                ColorSwatch(
                    name = name,
                    color = color,
                    selected = current == color,
                    tag = "${tagPrefix}_${name.lowercase()}"
                ) { onPick(color) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color?, selected: Boolean, tag: String, onClick: () -> Unit) {
    val palette = LocalDespiertaPalette.current
    val fill = color ?: palette.accent
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (color == null) Color.White.copy(alpha = 0.10f) else fill)
            .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.15f)), CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = "Color $name" }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        when {
            color == null -> Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            selected -> Icon(Icons.Rounded.Check, contentDescription = null, tint = contrastOn(fill), modifier = Modifier.size(18.dp))
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
    val border by animateColorAsState(
        if (selected) palette.accent else Color.White.copy(alpha = 0.12f),
        tween(motion(200)),
        label = "preset_border"
    )
    Column(
        modifier
            .clip(shape)
            .background(preset.skyBrush)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, border), shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag("theme_${preset.id}")
    ) {
        Box {
            NightLandscape(hill = preset.hill, modifier = Modifier.fillMaxWidth().height(52.dp))
            if (selected) {
                Box(
                    Modifier
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            preset.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

// ---------- Alarmas ----------

@Composable
private fun AlarmSettings(defaultSnooze: Int, onDefaultSnoozeChanged: (Int) -> Unit) {
    SettingsGroup("REPETICIÓN") {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StyledIcon(Icons.Rounded.Snooze)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    SettingTitle("Duración para alarmas nuevas")
                    SettingSubtitle("Cada alarma puede cambiarla en su editor")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SNOOZE_OPTIONS.forEach { minutes ->
                    ChoicePill(
                        text = "$minutes min",
                        selected = minutes == defaultSnooze,
                        onClick = { onDefaultSnoozeChanged(minutes) },
                        modifier = Modifier.testTag("default_snooze_$minutes")
                    )
                }
            }
        }
    }
    SettingsGroup("COMPORTAMIENTO") {
        InfoRow(Icons.Rounded.Schedule, "Formato de 12 horas", "Todas las horas se muestran con AM y PM")
        RowDivider()
        InfoRow(Icons.Rounded.CheckCircle, "Eliminar con confirmación", "Se pide confirmar y puedes deshacerlo justo después")
    }
}

// ---------- Sonidos y vibración ----------

@Composable
private fun SoundSettings(hapticsEnabled: Boolean, onHapticsChanged: (Boolean) -> Unit, onOpenNotifications: () -> Unit) {
    SettingsGroup("CONTROLES") {
        SwitchRow(
            icon = Icons.Rounded.Vibration,
            title = "Respuesta háptica",
            subtitle = "Vibración breve al usar los controles",
            checked = hapticsEnabled,
            onChanged = onHapticsChanged,
            tag = "haptics_switch"
        )
    }
    SettingsGroup("ALARMAS") {
        InfoRow(Icons.Rounded.MusicNote, "Tono de cada alarma", "Se elige en el editor de la alarma")
        RowDivider()
        InfoRow(Icons.Rounded.Vibration, "Vibración de cada alarma", "Se activa o desactiva en el editor")
        RowDivider()
        InfoRow(Icons.Rounded.VolumeUp, "Volumen bajo tu control", "Despierta no cambia el volumen ni ignora No molestar")
    }
    SettingsGroup("RECORDATORIOS") {
        NavigationRow(
            icon = Icons.Rounded.NotificationsActive,
            title = "Sonido y vibración de los avisos",
            subtitle = "Se configuran en Notificaciones",
            onClick = onOpenNotifications
        )
    }
}

// ---------- Notificaciones ----------

@Composable
private fun NotificationSettings(
    prefs: NotificationPrefs,
    canPost: Boolean,
    onChanged: (NotificationPrefs) -> Unit,
    onRequestNotifications: () -> Unit
) {
    val palette = LocalDespiertaPalette.current
    if (!canPost) {
        GlowCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            contentPadding = 14.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StyledIcon(Icons.Rounded.WarningAmber, tint = MaterialTheme.colorScheme.error)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    SettingTitle("Android no permite notificaciones")
                    SettingSubtitle("Mientras estén desactivadas no se mostrarán recordatorios ni avisos.")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRequestNotifications, modifier = Modifier.testTag("notify_permission")) {
                    Text("Permitir notificaciones", color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    SettingsGroup("AVISOS OPCIONALES") {
        SwitchRow(
            icon = Icons.Rounded.NotificationsActive,
            title = "Notificaciones opcionales",
            subtitle = "Recordatorios además de las alarmas",
            checked = prefs.enabled,
            onChanged = { onChanged(prefs.copy(enabled = it)) },
            tag = "notify_master"
        )
    }
    AnimatedVisibility(visible = prefs.enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsGroup("QUÉ AVISAR") {
                SwitchRow(
                    icon = Icons.Rounded.Bedtime,
                    title = "Recordatorio antes de dormir",
                    subtitle = "Según tu horario de Sueño",
                    checked = prefs.bedtimeReminder,
                    onChanged = { onChanged(prefs.copy(bedtimeReminder = it)) },
                    tag = "notify_bedtime"
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Rounded.Alarm,
                    title = "Aviso de próxima alarma",
                    subtitle = "Te recuerda la siguiente alarma programada",
                    checked = prefs.upcomingAlarm,
                    onChanged = { onChanged(prefs.copy(upcomingAlarm = it)) },
                    tag = "notify_upcoming"
                )
            }
            SettingsGroup("ANTICIPACIÓN") {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REMINDER_LEAD_OPTIONS.forEach { minutes ->
                        ChoicePill(
                            text = "$minutes min",
                            selected = minutes == prefs.leadMinutes,
                            onClick = { onChanged(prefs.copy(leadMinutes = minutes)) },
                            modifier = Modifier.testTag("notify_lead_$minutes")
                        )
                    }
                }
            }
            SettingsGroup("SONIDO Y VIBRACIÓN") {
                SwitchRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Sonido de recordatorio",
                    subtitle = "Usa el sonido de notificación del teléfono",
                    checked = prefs.sound,
                    onChanged = { onChanged(prefs.copy(sound = it)) },
                    tag = "notify_sound"
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Vibración",
                    subtitle = "Vibra al mostrar el aviso",
                    checked = prefs.vibrate,
                    onChanged = { onChanged(prefs.copy(vibrate = it)) },
                    tag = "notify_vibrate"
                )
            }
        }
    }
    FootNote(
        "Las alarmas suenan siempre, aunque desactives estos avisos. Android puede retrasar los " +
            "recordatorios unos minutos para ahorrar batería, y el modo No molestar puede silenciarlos."
    )
}

// ---------- Sueño ----------

@Composable
private fun SleepSettings(
    schedule: SleepSchedule,
    onScheduleChanged: (SleepSchedule) -> Unit,
    notifications: NotificationPrefs,
    onNotificationsChanged: (NotificationPrefs) -> Unit,
    onOpenSleep: () -> Unit
) {
    SettingsGroup("HORARIO") {
        NavigationRow(
            icon = Icons.Rounded.NightsStay,
            title = "Horario de sueño",
            subtitle = "${formatMinuteOfDay(schedule.plan.bedMinutes, false)} → " +
                formatMinuteOfDay(schedule.plan.wakeMinutes, false),
            onClick = onOpenSleep,
            modifier = Modifier.testTag("settings_open_sleep")
        )
    }
    SettingsGroup("OBJETIVO DE SUEÑO") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SLEEP_GOAL_OPTIONS.forEach { goal ->
                ChoicePill(
                    text = formatHoursMinutes(goal),
                    selected = goal == schedule.goalMinutes,
                    onClick = { onScheduleChanged(schedule.copy(goalMinutes = goal)) }
                )
            }
        }
    }
    SettingsGroup("RECORDATORIO") {
        SwitchRow(
            icon = Icons.Rounded.Bedtime,
            title = "Recordatorio para dormir",
            subtitle = "${notifications.leadMinutes} min antes de tu hora de dormir",
            checked = notifications.enabled && notifications.bedtimeReminder,
            onChanged = { on ->
                onNotificationsChanged(
                    if (on) notifications.copy(enabled = true, bedtimeReminder = true)
                    else notifications.copy(bedtimeReminder = false)
                )
            },
            tag = "settings_sleep_reminder"
        )
    }
    FootNote("Despierta no registra tu sueño. La duración y el progreso son estimaciones según tu horario.")
}

// ---------- Accesibilidad ----------

@Composable
private fun AccessibilitySettings(choice: ThemeChoice, onChanged: (ThemeChoice) -> Unit) {
    val options = LocalUiOptions.current
    SettingsGroup("TAMAÑO DEL TEXTO") {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill("Normal", !choice.largeText, { onChanged(choice.copy(largeText = false)) }, Modifier.testTag("text_size_normal"))
            ChoicePill("Grande", choice.largeText, { onChanged(choice.copy(largeText = true)) }, Modifier.testTag("text_size_large"))
        }
    }
    SettingsGroup("ANIMACIONES") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MotionLevel.entries.forEach { level ->
                ChoicePill(
                    text = level.label,
                    selected = level == choice.motion,
                    onClick = { onChanged(choice.copy(motion = level)) },
                    modifier = Modifier.testTag("motion_${level.name.lowercase()}")
                )
            }
        }
        if (options.systemReducedMotion) {
            RowDivider()
            InfoRow(
                Icons.Rounded.AutoAwesome,
                "Movimiento reducido en Android",
                "Android tiene las animaciones desactivadas y Despierta lo respeta"
            )
        }
    }
}

// ---------- Fiabilidad ----------

@Composable
private fun ReliabilitySettings(
    permissionState: PermissionUiState,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onRequestAlarmChannel: () -> Unit
) {
    SettingsGroup("PERMISOS") {
        if (permissionState.pendingCount == 0) {
            InfoRow(Icons.Rounded.CheckCircle, "Todo listo", "Android permite notificar y mostrar tus alarmas.")
        } else {
            val rows = buildList {
                if (permissionState.needsExactAlarm) add(Triple("Alarmas exactas", "Permite registrar la hora exacta incluso durante Doze", onRequestExactAlarm))
                if (permissionState.needsNotifications) add(Triple("Notificaciones", "Actívalas para ver Detener y Posponer mientras suena", onRequestNotifications))
                if (permissionState.needsFullScreen) add(Triple("Pantalla completa", "Autoriza mostrar la alarma sobre la pantalla bloqueada", onRequestFullScreen))
                if (permissionState.needsAlarmChannel) add(Triple("Canal sin prioridad alta", "Restaura la prioridad de Alarmas activas en Android", onRequestAlarmChannel))
            }
            rows.forEachIndexed { index, (title, detail, action) ->
                if (index > 0) RowDivider()
                ActionRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = title,
                    subtitle = detail,
                    onClick = action,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    SettingsGroup("RECUPERACIÓN") {
        InfoRow(
            Icons.Rounded.Shield,
            "Recuperación automática",
            "Se reprograman tras reiniciar, actualizar o cambiar la hora. Algunos fabricantes requieren permitir actividad en segundo plano."
        )
    }
}

// ---------- Información ----------

@Composable
private fun AboutSettings() {
    val palette = LocalDespiertaPalette.current
    SettingsGroup("DESPIERTA") {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.accentBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = palette.onAccent)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                SettingTitle("Despierta ${BuildConfig.VERSION_NAME}")
                SettingSubtitle("Compilación ${BuildConfig.VERSION_CODE} · ${BuildConfig.GIT_COMMIT}")
            }
        }
    }
    SettingsGroup("PRIVACIDAD") {
        InfoRow(Icons.Rounded.Lock, "Tus datos se quedan aquí", "Alarmas y ajustes se guardan solo en este teléfono.")
    }
}

// ---------- Piezas comunes ----------

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val palette = LocalDespiertaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = palette.textSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
        GlowCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun SettingTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LocalDespiertaPalette.current.textPrimary)
}

@Composable
private fun SettingSubtitle(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = LocalDespiertaPalette.current.textSoft)
}

@Composable
private fun FootNote(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = LocalDespiertaPalette.current.textSoft.copy(alpha = 0.85f),
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textSoft)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon, tint = tint)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = LocalDespiertaPalette.current.textSoft)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    tag: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChanged)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(icon)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            SettingTitle(title)
            SettingSubtitle(subtitle)
        }
        Switch(checked = checked, onCheckedChange = null, colors = despiertaSwitchColors())
    }
}

