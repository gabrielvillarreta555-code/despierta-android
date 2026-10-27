package com.example.despierta

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

class DespiertaSettingsTest {
    private val zone = ZoneId.of("America/Panama")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun calendarDaysMatchJavaTime() {
        assertEquals(Calendar.MONDAY, calendarDayOf(DayOfWeek.MONDAY))
        assertEquals(Calendar.SATURDAY, calendarDayOf(DayOfWeek.SATURDAY))
        assertEquals(Calendar.SUNDAY, calendarDayOf(DayOfWeek.SUNDAY))
    }

    @Test
    fun goalProgressIsAnEstimateCappedAtOne() {
        val plan = SleepPlan(bedMinutes = 23 * 60, wakeMinutes = 6 * 60) // 7 h
        assertEquals(7f / 8f, SleepSchedule(plan, goalMinutes = 8 * 60).goalProgress, 0.0001f)
        assertEquals(1f, SleepSchedule(plan, goalMinutes = 6 * 60).goalProgress, 0.0001f)
        assertEquals("7 h", formatHoursMinutes(7 * 60))
        assertEquals("7 h 30 min", formatHoursMinutes(7 * 60 + 30))
    }

    @Test
    fun bedtimeReminderFiresBeforeBedtimeOnSelectedDaysOnly() {
        // Martes 27 de octubre de 2026, 20:00. Dormir a las 11:00 PM, aviso 15 min antes.
        val now = millis(2026, 10, 27, 20, 0)
        val everyDay = SleepSchedule(SleepPlan(23 * 60, 6 * 60 + 30))
        val at = nextBedtimeReminderMillis(everyDay, leadMinutes = 15, nowMillis = now, zone = zone)
        assertEquals(millis(2026, 10, 27, 22, 45), at)

        // Solo viernes: salta al viernes 30.
        val fridays = everyDay.copy(days = setOf(Calendar.FRIDAY))
        assertEquals(
            millis(2026, 10, 30, 22, 45),
            nextBedtimeReminderMillis(fridays, leadMinutes = 15, nowMillis = now, zone = zone)
        )

        // Ya pasó la hora del aviso de hoy: el siguiente es mañana.
        val late = millis(2026, 10, 27, 22, 50)
        assertEquals(
            millis(2026, 10, 28, 22, 45),
            nextBedtimeReminderMillis(everyDay, leadMinutes = 15, nowMillis = late, zone = zone)
        )

        assertNull(nextBedtimeReminderMillis(everyDay.copy(days = emptySet()), 15, now, zone))
    }

    @Test
    fun upcomingAlarmNoticeRespectsLeadTimeOrWaitsForTheAlarm() {
        val trigger = millis(2026, 10, 28, 6, 30)
        val early = upcomingAlarmNotice(trigger, leadMinutes = 30, nowMillis = millis(2026, 10, 28, 5, 0))
        assertTrue(early.postNotice)
        assertEquals(millis(2026, 10, 28, 6, 0), early.atMillis)
        val text = Instant.ofEpochMilli(trigger).atZone(zone).let { formatClockTime(it.hour, it.minute) }
        assertEquals("6:30 AM", text)

        // Dentro de la anticipación: no avisa tarde; vuelve a calcular cuando ya haya sonado.
        val late = upcomingAlarmNotice(trigger, leadMinutes = 30, nowMillis = millis(2026, 10, 28, 6, 10))
        assertFalse(late.postNotice)
        assertTrue(late.atMillis > trigger)
    }

    @Test
    fun selectableColorsKeepTextReadable() {
        for ((name, color) in DespiertaAccents) {
            // Texto sobre botones y acentos (WCAG AA para texto normal: 4.5).
            assertTrue("$name sobre texto", contrastRatio(color, contrastOn(color)) >= 4.5f)
            // Iconos y controles sobre las tarjetas oscuras de cada fondo (mínimo 3 para elementos gráficos).
            for (preset in DespiertaPresets) {
                for (card in preset.card) {
                    assertTrue("$name sobre ${preset.name}", contrastRatio(color, card) >= 3f)
                }
            }
        }
        // Los textos principales sobre las tarjetas también se leen bien.
        for (preset in DespiertaPresets) {
            val palette = preset
            for (card in preset.card) {
                assertTrue(contrastRatio(palette.textPrimary, card) >= 7f)
                assertTrue(contrastRatio(palette.textSoft, card) >= 4.5f)
            }
        }
        assertEquals(Color.White, contrastOn(Color(0xFF1C1634)))
    }

    @Test
    fun themeOverridesApplyAndDefaultRestoresPreset() {
        val custom = ThemeChoice(
            presetId = "bosque",
            accent = Color(0xFF82B1FF),
            navIconColor = Color(0xFFF48FB1),
            cardIconColor = Color(0xFFFFB74D),
            buttonColor = Color(0xFF8EE3BF),
            iconStyle = IconStyle.SOFT,
            largeText = true,
            motion = MotionLevel.OFF
        )
        val palette = custom.toPalette()
        assertEquals("bosque", palette.id)
        assertEquals(Color(0xFF82B1FF), palette.accent)
        assertEquals(Color(0xFFF48FB1), palette.navIconSelected)
        assertEquals(Color(0xFFFFB74D), palette.cardIcon)
        assertEquals(Color(0xFF8EE3BF), palette.button)

        val restored = ThemeChoice.DEFAULT.toPalette()
        assertEquals("noche", restored.id)
        assertEquals(DespiertaPresets.first().accent, restored.button)
        assertEquals(0, UiOptions(motionFactor = MotionLevel.OFF.factor).millis(220))
    }
}
