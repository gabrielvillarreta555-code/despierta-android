package com.example.despierta

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepPlanTest {
    @Test
    fun durationCrossesMidnight() {
        assertEquals(7 * 60 + 30, sleepPlanDuration(bedMinutes = 23 * 60, wakeMinutes = 6 * 60 + 30))
        assertEquals(7 * 60 + 30, SleepPlan(bedMinutes = 0, wakeMinutes = 7 * 60 + 30).durationMinutes)
    }

    @Test
    fun durationWithinSameDayForDaySleepers() {
        assertEquals(6 * 60, sleepPlanDuration(bedMinutes = 8 * 60, wakeMinutes = 14 * 60))
    }

    @Test
    fun equalTimesMeanNoSleepWindow() {
        assertEquals(0, sleepPlanDuration(bedMinutes = 300, wakeMinutes = 300))
        assertEquals("Elige horas distintas para dormir y despertar.", sleepGuidance(0))
    }

    @Test
    fun minuteOfDayIsNormalized() {
        assertEquals(0, normalizeMinuteOfDay(MINUTES_PER_DAY))
        assertEquals(MINUTES_PER_DAY - 1, normalizeMinuteOfDay(-1))
    }

    @Test
    fun guidanceFollowsSevenToNineHourReference() {
        assertEquals(
            "Una ventana dentro de la referencia general de 7 a 9 horas para adultos.",
            sleepGuidance(8 * 60)
        )
        assertEquals(
            "Descanso corto: prepara el ambiente y reduce distracciones antes de dormir.",
            sleepGuidance(5 * 60)
        )
    }
}
