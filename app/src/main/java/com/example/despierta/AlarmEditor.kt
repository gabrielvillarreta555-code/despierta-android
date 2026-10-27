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

// Editor de alarmas. La lógica de validación vive en AlarmEditorState.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AlarmEditor(
    initial: AlarmModel,
    isNew: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmModel) -> Unit
) {
    // Estado y validaciones en AlarmEditorState; aquí solo se dibujan.
    val state = remember(initial.id) { AlarmEditorState(initial) }
    var label by state::label
    var days by state::days
    var soundUri by state::soundUri
    var vibrate by state::vibrate
    var snoozeMinutes by state::snoozeMinutes
    var mathChallenge by state::mathChallenge
    var useShift by state::useShift
    var workDays by state::workDays
    var restDays by state::restDays
    var startEpochDay by state::startEpochDay
    val context = LocalContext.current
    // Toda la app usa formato de 12 horas con AM/PM, sin depender del ajuste del teléfono.
    val uses24HourClock = state.uses24HourClock
    var selectedHour by state::selectedHour
    var selectedMinute by state::selectedMinute
    var manualEntryVisible by state::manualEntryVisible
    var manualTime by state::manualTime
    val shiftPatternCandidate = state.shiftPatternCandidate
    val shiftInputValid = state.shiftInputValid
    val manualTimeValid = state.manualTimeValid
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
        state.toAlarm()?.let(onSave)
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
                    Button(
                        onClick = ::save,
                        enabled = shiftInputValid && manualTimeValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalDespiertaPalette.current.button,
                            contentColor = LocalDespiertaPalette.current.onButton
                        ),
                        modifier = Modifier.testTag("editor_save")
                    ) {
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
