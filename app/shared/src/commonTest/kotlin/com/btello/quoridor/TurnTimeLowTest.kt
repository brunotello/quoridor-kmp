package com.btello.quoridor

import com.btello.quoridor.presentation.game.isTurnTimeLow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnTimeLowTest {

    @Test
    fun plentyOfTimeIsNotLow() {
        assertFalse(isTurnTimeLow(30_000L))
        assertFalse(isTurnTimeLow(10_001L))
    }

    @Test
    fun tenSecondsOrLessIsLow() {
        assertTrue(isTurnTimeLow(10_000L))
        assertTrue(isTurnTimeLow(1L))
    }

    @Test
    fun expiredTimeIsLow() {
        assertTrue(isTurnTimeLow(0L))
    }
}
