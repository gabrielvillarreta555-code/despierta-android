package com.example.despierta

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * Estado y validaciones del editor de alarmas, separados de su representación visual.
 *
 * El editor solo dibuja estos valores; aquí viven las reglas que no deben perderse al
 * rediseñar: turno válido, hora manual válida, normalización de la repetición y cómo se
 * construye la alarma que se guarda. No programa nada: quien guarda y programa es MainActivity.
 */
@Stable
internal class AlarmEditorState(
    val initial: AlarmModel,
    today: LocalDate = LocalDate.now(),
    /** Toda la app usa formato de 12 horas con AM/PM. */
    val uses24HourClock: Boolean = false
) {
    private val validInitialShift = initial.shiftPattern?.takeIf { it.isValid }

    var label by mutableStateOf(initial.label)
    var days by mutableStateOf(initial.days)
    var soundUri by mutableStateOf(initial.soundUri)
    var vibrate by mutableStateOf(initial.vibrate)
    var snoozeMinutes by mutableIntStateOf(AlarmStore.normalizeSnoozeMinutes(initial.snoozeMinutes))
    var mathChallenge by mutableStateOf(initial.mathChallengeEnabled)
    var useShift by mutableStateOf(initial.shiftPattern != null)
    var workDays by mutableStateOf(validInitialShift?.workDays?.toString() ?: DEFAULT_WORK_DAYS)
    var restDays by mutableStateOf(validInitialShift?.restDays?.toString() ?: DEFAULT_REST_DAYS)
    var startEpochDay by mutableLongStateOf(validInitialShift?.startEpochDay ?: today.toEpochDay())
    var selectedHour by mutableIntStateOf(initial.hour)
    var selectedMinute by mutableIntStateOf(initial.minute)
    var manualEntryVisible by mutableStateOf(false)
    var manualTime by mutableStateOf(formatManualTime(initial.hour, initial.minute, uses24HourClock))

    /** Patrón de turno según lo escrito, o null si no hay turno o los números no son válidos. */
    val shiftPatternCandidate: ShiftPattern?
        get() {
            if (!useShift) return null
            val work = workDays.toIntOrNull() ?: return null
            val rest = restDays.toIntOrNull() ?: return null
            return ShiftPattern(
                startEpochDay = startEpochDay,
                workDays = work,
                restDays = rest,
                exceptionEpochDays = validInitialShift?.exceptionEpochDays ?: emptySet()
            )
        }

    val shiftInputValid: Boolean
        get() = !useShift || shiftPatternCandidate?.isValid == true

    val manualTimeValid: Boolean
        get() = !manualEntryVisible || parseManualTime(manualTime, uses24HourClock) != null

    val canSave: Boolean
        get() = shiftInputValid && manualTimeValid

    /** Hora elegida con las ruedas: también actualiza el texto de la entrada manual. */
    fun setTime(hour24: Int, minute: Int) {
        selectedHour = hour24
        selectedMinute = minute
        manualTime = formatManualTime(hour24, minute, uses24HourClock)
    }

    fun toggleManualEntry() {
        manualEntryVisible = !manualEntryVisible
        manualTime = formatManualTime(selectedHour, selectedMinute, uses24HourClock)
    }

    /** Texto escrito a mano: solo cambia la hora si es válido. */
    fun onManualTimeChanged(entered: String) {
        manualTime = entered.take(MAX_MANUAL_LENGTH)
        parseManualTime(entered, uses24HourClock)?.let { parsed ->
            selectedHour = parsed.hour24
            selectedMinute = parsed.minute
        }
    }

    /**
     * Alarma lista para guardar, o null si el turno no es válido. Igual que antes del
     * rediseño: se activa, se borra cualquier posposición pendiente y el nombre vacío pasa
     * a ser "Alarma".
     */
    fun toAlarm(): AlarmModel? {
        val pattern = shiftPatternCandidate
        if (useShift && pattern == null) return null
        return initial.copy(
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
    }

    private companion object {
        const val DEFAULT_WORK_DAYS = "12"
        const val DEFAULT_REST_DAYS = "4"
        const val MAX_MANUAL_LENGTH = 11
    }
}
