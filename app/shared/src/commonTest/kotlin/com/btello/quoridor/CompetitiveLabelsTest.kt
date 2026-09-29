package com.btello.quoridor

import com.btello.quoridor.presentation.online.formatClock
import kotlin.test.Test
import kotlin.test.assertEquals

class CompetitiveLabelsTest {

    @Test
    fun `formats whole minutes and seconds as m colon ss`() {
        assertEquals("5:00", formatClock(300_000L))
        assertEquals("0:09", formatClock(9_000L))
        assertEquals("10:00", formatClock(600_000L))
    }

    @Test
    fun `rounds partial seconds up so the clock only hits zero when depleted`() {
        assertEquals("3:05", formatClock(184_001L))
        assertEquals("0:01", formatClock(1L))
    }

    @Test
    fun `never renders negative time`() {
        assertEquals("0:00", formatClock(0L))
        assertEquals("0:00", formatClock(-5_000L))
    }
}
