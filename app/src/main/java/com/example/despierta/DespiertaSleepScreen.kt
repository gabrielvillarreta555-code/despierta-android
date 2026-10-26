package com.example.despierta

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

private enum class SleepTarget { BEDTIME, WAKE }

/**
 * Pantalla de horas de sueño. Solo calcula y guarda una preferencia visual;
 * para crear la alarma reutiliza el editor existente, que es quien guarda y programa.
 */
@Composable
internal fun SleepScreen(
    plan: SleepPlan,
    alarms: List<AlarmModel>,
    nowMillis: Long,
    hapticsEnabled: Boolean,
    onPlanChanged: (SleepPlan) -> Unit,
    onCreateWakeAlarm: (hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    var target by rememberSaveable { mutableStateOf(SleepTarget.WAKE) }
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
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp)
            .testTag("sleep_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Lavender.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            ) {
                IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Volver a alarmas", tint = LavenderSoft)
                }
            }
            Text(
                "Horas de sueño",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 14.dp)
            )
        }

        Box(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SleepDial(plan = plan, modifier = Modifier.fillMaxSize().testTag("sleep_dial"))
                val duration = plan.durationMinutes
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%02d".format(duration / 60),
                            fontSize = 54.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("sleep_dial_hours")
                        )
                        Text(
                            "h",
                            fontSize = 20.sp,
                            color = TextSoft,
                            modifier = Modifier.padding(start = 3.dp, bottom = 8.dp)
                        )
                    }
                    Text("%d min".format(duration % 60), fontSize = 16.sp, color = TextSoft)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SleepTimeCard(
                title = "Dormir",
                minutes = plan.bedMinutes,
                is24Hour = is24Hour,
                icon = Icons.Rounded.Bedtime,
                selected = target == SleepTarget.BEDTIME,
                onClick = { target = SleepTarget.BEDTIME },
                modifier = Modifier.weight(1f).testTag("sleep_bedtime_card")
            )
            SleepTimeCard(
                title = "Despertar",
                minutes = plan.wakeMinutes,
                is24Hour = is24Hour,
                icon = Icons.Rounded.WbSunny,
                selected = target == SleepTarget.WAKE,
                onClick = { target = SleepTarget.WAKE },
                modifier = Modifier.weight(1f).testTag("sleep_wake_card")
            )
        }

        GlowCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Text(
                if (target == SleepTarget.BEDTIME) "AJUSTA LA HORA DE DORMIR" else "AJUSTA LA HORA DE DESPERTAR",
                color = Lavender,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            val editing = if (target == SleepTarget.BEDTIME) plan.bedMinutes else plan.wakeMinutes
            // La clave recrea el selector al cambiar entre dormir y despertar.
            key(target) {
                TimeWheelPicker(
                    hour24 = editing / 60,
                    minute = editing % 60,
                    is24Hour = is24Hour,
                    hapticsEnabled = hapticsEnabled,
                    onTimeChanged = { hour, minute ->
                        val value = normalizeMinuteOfDay(hour * 60 + minute)
                        onPlanChanged(
                            if (target == SleepTarget.BEDTIME) plan.copy(bedMinutes = value)
                            else plan.copy(wakeMinutes = value)
                        )
                    }
                )
            }
        }

        GlowCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            Text(sleepGuidance(plan.durationMinutes), color = TextSoft, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(14.dp))
            if (matchingAlarm != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Lavender)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Ya tienes «${matchingAlarm.label}» activa a las ${formatMinuteOfDay(plan.wakeMinutes, is24Hour)}.",
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("sleep_alarm_matched")
                    )
                }
            } else {
                Button(
                    onClick = { onCreateWakeAlarm(wakeHour, wakeMinute) },
                    colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = OnLavender),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("sleep_create_alarm")
                ) {
                    Icon(Icons.Rounded.AlarmAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Crear alarma a las ${formatMinuteOfDay(plan.wakeMinutes, is24Hour)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Se abrirá el editor para que confirmes sonido, repetición y posponer.",
                    color = TextSoft,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            if (next != null) {
                val nextMinutes = Instant.ofEpochMilli(next.triggerAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .let { it.hour * 60 + it.minute }
                if (nextMinutes != plan.wakeMinutes) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onPlanChanged(plan.copy(wakeMinutes = nextMinutes)) },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Lavender.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sleep_use_next_alarm")
                    ) {
                        Text(
                            "Usar mi próxima alarma (${formatMinuteOfDay(nextMinutes, is24Hour)})",
                            color = LavenderSoft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimeCard(
    title: String,
    minutes: Int,
    is24Hour: Boolean,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val background: Brush = if (selected) LavenderBrush else CardBrush
    val foreground = if (selected) OnLavender else Color.White
    val iconTint = if (icon == Icons.Rounded.WbSunny) DawnOrange else DuskBlue
    Column(
        modifier
            .clip(shape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.25f else 0.07f), shape)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 12.sp, color = foreground.copy(alpha = 0.78f))
        Text(
            formatMinuteOfDay(minutes, is24Hour),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
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
