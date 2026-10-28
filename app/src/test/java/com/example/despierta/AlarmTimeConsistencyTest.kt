package com.example.despierta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * La hora elegida, la guardada, la mostrada y la programada deben ser la misma.
 */
class AlarmTimeConsistencyTest {

    @Test
    fun twelveHourFormatShowsAmAndPm() {
        assertEquals("12:00 AM", formatClockTime(0, 0))
        assertEquals("12:00 PM", formatClockTime(12, 0))
        assertEquals("1:00 PM", formatClockTime(13, 0))
        assertEquals("11:59 AM", formatClockTime(11, 59))
        assertEquals("11:59 PM", formatClockTime(23, 59))
        assertEquals("7:05 AM", formatClockTime(7, 5))
        assertEquals("1:05" to "PM", clockParts(13, 5))
    }

    @Test
    fun everyTimeOfDayRoundTripsThroughWheelAndText() {
        for (hour24 in 0..23) {
            for (minute in listOf(0, 1, 30, 59)) {
                // Rueda de 12 horas: hora 1–12 + periodo AM/PM.
                val wheelHour = hour12From24(hour24)
                val isPm = hour24 >= 12
                assertEquals(hour24, hour24From12(wheelHour, isPm))
                assertEquals(hour24, selectWheelHour(hour24, wheelHour, is24Hour = false))
                assertEquals(hour24, selectWheelPeriod(hour24, isPm))

                // Texto mostrado y entrada manual.
                val shown = formatClockTime(hour24, minute)
                assertEquals(SelectedTime(hour24, minute), parseManualTime(shown, is24Hour = false))
                assertEquals(shown, formatManualTime(hour24, minute, is24Hour = false))
            }
        }
    }

    @Test
    fun wheelPicksTheCenteredRowEvenWhenSnapStopsAFewPixelsShort() {
        val itemPx = 137 // 52 dp a densidad 2.625 (420 dpi), alto no entero en píxeles.
        // Ajuste exacto: la fila centrada es la primera.
        assertEquals(11, centeredWheelIndex(11, 0, itemPx, 12))
        // El ajuste quedó 1 px corto: la fila 10 (hora 11) asoma, pero la centrada es la 11 (hora 12).
        assertEquals(11, centeredWheelIndex(10, 136, itemPx, 12))
        // Medio desplazamiento o más pasa a la siguiente; menos se queda.
        assertEquals(11, centeredWheelIndex(10, 69, itemPx, 12))
        assertEquals(10, centeredWheelIndex(10, 68, itemPx, 12))
        // Nunca sale del rango.
        assertEquals(11, centeredWheelIndex(11, 136, itemPx, 12))
        assertEquals(0, centeredWheelIndex(0, 0, 0, 12))
    }

    @Test
    fun hourTwelveFromTheTwelveHourWheelIsNoonNotEleven() {
        val hourEntries = (1..12).toList()
        val centered = centeredWheelIndex(10, 136, 137, hourEntries.size)
        val selected = selectWheelHour(currentHour24 = 11, wheelHour = hourEntries[centered], is24Hour = false)
        // Rueda en AM: 12 AM es medianoche. Con PM es mediodía.
        assertEquals(0, selected)
        assertEquals(12, selectWheelPeriod(selected, isPm = true))
        assertEquals("12:00 PM", formatClockTime(selectWheelPeriod(selected, isPm = true), 0))
    }

    @Test
    fun noonAlarmIsScheduledForNoonLocalTime() {
        val zones = listOf("America/Panama", "America/New_York", "Europe/Madrid", "UTC")
        for (zoneName in zones) {
            val zone = ZoneId.of(zoneName)
            val now = LocalDateTime.of(2026, 10, 27, 9, 0).atZone(zone).toInstant().toEpochMilli()
            val trigger = AlarmScheduler.nextTriggerMillis(alarmAt(12, 0), now, TimeZone.getTimeZone(zone))
            val local = Instant.ofEpochMilli(trigger).atZone(zone)
            assertEquals("$zoneName hora", 12, local.hour)
            assertEquals("$zoneName minuto", 0, local.minute)
            assertEquals(LocalDate.of(2026, 10, 27), local.toLocalDate())
            assertTrue(formatOccurrence(trigger, zone).endsWith("12:00 PM"))
        }
    }

    @Test
    fun noonAlarmKeepsItsHourAcrossDaylightSavingChange() {
        // 1 de noviembre de 2026: Nueva York pasa de horario de verano a estándar.
        val zone = ZoneId.of("America/New_York")
        val now = LocalDateTime.of(2026, 10, 31, 13, 0).atZone(zone).toInstant().toEpochMilli()
        val trigger = AlarmScheduler.nextTriggerMillis(alarmAt(12, 0), now, TimeZone.getTimeZone(zone))
        val local = Instant.ofEpochMilli(trigger).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 1), local.toLocalDate())
        assertEquals(12, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun shiftAlarmAtNoonIsScheduledForNoonLocalTime() {
        val zone = ZoneId.of("America/Panama")
        val start = LocalDate.of(2026, 10, 27)
        val pattern = ShiftPattern.from(start, 12, 4)
        val now = LocalDateTime.of(2026, 10, 27, 9, 0).atZone(zone).toInstant().toEpochMilli()
        val trigger = AlarmScheduler.nextTriggerMillis(
            alarmAt(12, 0).copy(shiftPattern = pattern),
            now,
            TimeZone.getTimeZone(zone)
        )
        val local = Instant.ofEpochMilli(trigger).atZone(zone)
        assertEquals(start, local.toLocalDate())
        assertEquals(12, local.hour)
        assertEquals(0, local.minute)
    }

    private fun alarmAt(hour: Int, minute: Int) = AlarmModel(
        id = 321,
        hour = hour,
        minute = minute,
        label = "Mediodía",
        soundUri = "content://test/alarm",
        enabled = true
    )
}
