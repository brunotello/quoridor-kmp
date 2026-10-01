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

        val timed = CompetitiveConfig(turnTimeSeconds = 30)
        assertTrue(timed.hasTimer)
        assertTrue(timed.isCompetitive)
    }

    @Test
    fun configRejectsNonPositiveTurnTime() {
        assertFailsWith<IllegalArgumentException> {
            CompetitiveConfig(turnTimeSeconds = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            CompetitiveConfig(turnTimeSeconds = -5)
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
    fun initialStartsFirstRoundWithoutWins() {
        val state = CompetitiveState.initial(
            playerCount = 4,
            config = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3, turnTimeSeconds = 45),
        )
        assertEquals(listOf(0, 0, 0, 0), state.wins)
        assertEquals(0, state.gameIndex)
        assertFalse(state.isSeriesOver)
        assertNull(state.seriesWinner)
    }

    @Test
    fun remainingTurnMillisCountsDownFromTurnTime() {
        val config = CompetitiveConfig(turnTimeSeconds = 30)
        assertEquals(30_000L, config.remainingTurnMillis(0L))
        assertEquals(12_500L, config.remainingTurnMillis(17_500L))
        assertEquals(0L, config.remainingTurnMillis(30_000L))
    }

    @Test
    fun remainingTurnMillisNeverGoesNegative() {
        assertEquals(0L, CompetitiveConfig(turnTimeSeconds = 45).remainingTurnMillis(90_000L))
    }

    @Test
    fun remainingTurnMillisIsNullWithoutTimer() {
        assertNull(CompetitiveConfig().remainingTurnMillis(10_000L))
    }

    @Test
    fun turnExpiresOnlyWhenTimeRunsOut() {
        val config = CompetitiveConfig(turnTimeSeconds = 60)
        assertFalse(config.isTurnExpired(59_999L))
        assertTrue(config.isTurnExpired(60_000L))
        assertTrue(config.isTurnExpired(75_000L))
    }

    @Test
    fun turnNeverExpiresWithoutTimer() {
        assertFalse(CompetitiveConfig().isTurnExpired(Long.MAX_VALUE))
    }

    @Test
    fun turnTimeOptionsAre30_45And60Seconds() {
        assertEquals(listOf(30, 45, 60), CompetitiveConfig.TURN_TIME_OPTIONS)
    }
}
