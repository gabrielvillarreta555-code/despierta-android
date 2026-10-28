package com.example.despierta

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUntilTest {
    private val minute = 60_000L

    @Test
    fun readableCountdown() {
        assertEquals("en 7 h 33 min", formatTimeUntil(7 * 60 * minute + 33 * minute))
        assertEquals("en 8 h", formatTimeUntil(8 * 60 * minute))
        assertEquals("en 12 min", formatTimeUntil(12 * minute))
        assertEquals("en menos de 1 min", formatTimeUntil(30_000L))
        assertEquals("en 1 d 2 h", formatTimeUntil((26 * 60) * minute))
        assertEquals("Ahora", formatTimeUntil(0L))
    }
}
