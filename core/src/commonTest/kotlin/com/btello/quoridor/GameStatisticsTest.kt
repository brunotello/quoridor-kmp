package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.domain.stats.GameStatistics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameStatisticsTest {

    private fun record(
        won: Boolean = true,
        difficulty: AiDifficulty? = AiDifficulty.EASY,
        durationMillis: Long = 1_000,
        moveCount: Int = 10,
        wallsUsed: Int = 2,
    ) = GameRecord(won, difficulty, durationMillis, moveCount, wallsUsed)

    @Test
    fun `empty records yield EMPTY statistics`() {
        val stats = GameStatistics.from(emptyList())
        assertEquals(GameStatistics.EMPTY, stats)
        assertEquals(0, stats.totalGames)
        assertNull(stats.bestTimeMillis)
        assertNull(stats.fewestMoves)
        assertNull(stats.fewestWalls)
    }

    @Test
    fun `total games counts every record`() {
        val stats = GameStatistics.from(listOf(record(), record(won = false), record()))
        assertEquals(3, stats.totalGames)
    }

    @Test
    fun `best metrics consider only won games`() {
        val stats = GameStatistics.from(
            listOf(
                record(won = true, durationMillis = 5_000, moveCount = 30, wallsUsed = 6),
                record(won = true, durationMillis = 2_000, moveCount = 20, wallsUsed = 3),
                // A lost game with better raw numbers must be ignored.
                record(won = false, durationMillis = 100, moveCount = 1, wallsUsed = 0),
            ),
        )
        assertEquals(2_000, stats.bestTimeMillis)
        assertEquals(20, stats.fewestMoves)
        assertEquals(3, stats.fewestWalls)
    }

    @Test
    fun `best metrics are null when no game was won`() {
        val stats = GameStatistics.from(listOf(record(won = false), record(won = false)))
        assertNull(stats.bestTimeMillis)
        assertNull(stats.fewestMoves)
        assertNull(stats.fewestWalls)
    }

    @Test
    fun `breakdown groups by difficulty and counts results`() {
        val stats = GameStatistics.from(
            listOf(
                record(won = true, difficulty = AiDifficulty.EASY),
                record(won = false, difficulty = AiDifficulty.EASY),
                record(won = true, difficulty = AiDifficulty.HARD),
                record(won = true, difficulty = null),
            ),
        )
        val easy = stats.breakdown.first { it.difficulty == AiDifficulty.EASY }
        assertEquals(2, easy.total)
        assertEquals(1, easy.wins)
        assertEquals(1, easy.losses)

        val hard = stats.breakdown.first { it.difficulty == AiDifficulty.HARD }
        assertEquals(1, hard.wins)
        assertEquals(0, hard.losses)

        val local = stats.breakdown.first { it.difficulty == null }
        assertEquals(1, local.wins)
    }

    @Test
    fun `breakdown is sorted by difficulty with local games last`() {
        val stats = GameStatistics.from(
            listOf(
                record(difficulty = null),
                record(difficulty = AiDifficulty.HARD),
                record(difficulty = AiDifficulty.EASY),
            ),
        )
        assertEquals(
            listOf(AiDifficulty.EASY, AiDifficulty.HARD, null),
            stats.breakdown.map { it.difficulty },
        )
    }
}
