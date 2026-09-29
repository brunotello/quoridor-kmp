package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompetitiveTest {

    @Test
    fun gamesToWinMatchesFormat() {
        assertEquals(1, SeriesFormat.SINGLE.gamesToWin)
        assertEquals(3, SeriesFormat.FIRST_TO_3.gamesToWin)
        assertEquals(5, SeriesFormat.FIRST_TO_5.gamesToWin)
    }

    @Test
    fun configFlagsReflectOptions() {
        val plain = CompetitiveConfig()
        assertFalse(plain.hasTimer)
        assertFalse(plain.isCompetitive)

        val series = CompetitiveConfig(format = SeriesFormat.FIRST_TO_5)
        assertFalse(series.hasTimer)
        assertTrue(series.isCompetitive)

        val timed = CompetitiveConfig(timeControlSeconds = 300)
        assertTrue(timed.hasTimer)
        assertTrue(timed.isCompetitive)
    }

    @Test
    fun configRejectsNonPositiveTimeControl() {
        assertFailsWith<IllegalArgumentException> {
            CompetitiveConfig(timeControlSeconds = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            CompetitiveConfig(timeControlSeconds = -5)
        }
    }

    @Test
    fun seriesIsOverWhenAPlayerReachesTarget() {
        val config = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3)
        assertFalse(CompetitiveState(config = config, wins = listOf(2, 2)).isSeriesOver)
        assertTrue(CompetitiveState(config = config, wins = listOf(3, 1)).isSeriesOver)
    }

    @Test
    fun seriesWinnerReturnsFirstPlayerReachingTarget() {
        val config = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3)
        assertNull(CompetitiveState(config = config, wins = listOf(2, 2)).seriesWinner)
        assertEquals(
            PlayerId(1),
            CompetitiveState(config = config, wins = listOf(2, 3)).seriesWinner,
        )
    }

    @Test
    fun initialWithoutTimerHasEmptyClocks() {
        val state = CompetitiveState.initial(
            playerCount = 2,
            config = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        assertEquals(listOf(0, 0), state.wins)
        assertEquals(0, state.gameIndex)
        assertTrue(state.remainingMillis.isEmpty())
        assertFalse(state.isSeriesOver)
    }

    @Test
    fun initialWithTimerSeedsEachPlayerClock() {
        val state = CompetitiveState.initial(
            playerCount = 2,
            config = CompetitiveConfig(timeControlSeconds = 300),
        )
        assertEquals(listOf(300_000L, 300_000L), state.remainingMillis)
    }
}
