package com.example.despierta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Calendar

/** Las reglas del editor deben seguir iguales aunque cambie su diseño. */
class AlarmEditorStateTest {
    private val today = LocalDate.of(2026, 10, 27)

    private fun alarm(
        hour: Int = 6,
        minute: Int = 30,
        label: String = "Trabajo",
        pattern: ShiftPattern? = null,
        snooze: Int = 10
    ) = AlarmModel(
        id = 77,
        hour = hour,
        minute = minute,
        label = label,
        days = setOf(Calendar.MONDAY),
        shiftPattern = pattern,
        soundUri = "content://test/alarm",
        snoozeMinutes = snooze,
        enabled = false,
        snoozedUntilMillis = 123L
    )

    @Test
    fun savingKeepsEditedValuesEnablesAndClearsSnooze() {
        val state = AlarmEditorState(alarm(), today)
        state.setTime(12, 0)
        state.label = "  "
        val saved = requireNotNull(state.toAlarm())
        assertEquals(12, saved.hour)
        assertEquals(0, saved.minute)
        assertEquals("Alarma", saved.label)
        assertEquals(setOf(Calendar.MONDAY), saved.days)
        assertTrue(saved.enabled)
        assertNull(saved.snoozedUntilMillis)
        assertEquals(10, saved.snoozeMinutes)
        assertEquals("12:00 PM", state.manualTime)
    }

    @Test
    fun unsupportedSnoozeFallsBackToDefault() {
        assertEquals(DEFAULT_SNOOZE_MINUTES, AlarmEditorState(alarm(snooze = 7), today).snoozeMinutes)
    }

    @Test
    fun manualEntryOnlyChangesTimeWhenValid() {
        val state = AlarmEditorState(alarm(), today)
        state.toggleManualEntry()
        assertTrue(state.manualTimeValid)
        state.onManualTimeChanged("1:05 pm")
        assertEquals(13, state.selectedHour)
        assertEquals(5, state.selectedMinute)
        state.onManualTimeChanged("25:99")
        assertFalse(state.manualTimeValid)
        assertFalse(state.canSave)
        assertEquals(13, state.selectedHour)
    }

    @Test
    fun shiftNeedsValidWorkAndRestDays() {
        val state = AlarmEditorState(alarm(), today)
        state.useShift = true
        assertTrue(state.shiftInputValid) // 12 x 4 por defecto
        assertEquals(today.toEpochDay(), state.shiftPatternCandidate?.startEpochDay)
        state.restDays = ""
        assertFalse(state.shiftInputValid)
        assertNull(state.toAlarm())
        state.restDays = "0"
        assertFalse(state.shiftInputValid)
        state.restDays = "7"
        state.workDays = "14"
        val saved = requireNotNull(state.toAlarm())
        assertEquals(emptySet<Int>(), saved.days)
        assertEquals(14, saved.shiftPattern?.workDays)
        assertEquals(7, saved.shiftPattern?.restDays)
    }

    @Test
    fun existingShiftKeepsItsStartAndExceptions() {
        val start = LocalDate.of(2026, 10, 20)
        val pattern = ShiftPattern.from(start, 14, 7).copy(exceptionEpochDays = setOf(start.plusDays(3).toEpochDay()))
        val state = AlarmEditorState(alarm(pattern = pattern), today)
        assertTrue(state.useShift)
        assertEquals("14", state.workDays)
        val saved = requireNotNull(state.toAlarm())
        assertEquals(start.toEpochDay(), saved.shiftPattern?.startEpochDay)
        assertEquals(pattern.exceptionEpochDays, saved.shiftPattern?.exceptionEpochDays)
    }
}
