package com.btello.quoridor

import com.btello.quoridor.data.stats.SettingsStatisticsRepository
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.stats.GameRecord
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsStatisticsRepositoryTest {

    private fun repository(settings: Settings = MapSettings()) =
        SettingsStatisticsRepository(settings)

    private fun record(
        won: Boolean = true,
        difficulty: AiDifficulty? = AiDifficulty.EASY,
    ) = GameRecord(won, difficulty, durationMillis = 1_000, moveCount = 10, wallsUsed = 2)

    @Test
    fun `records is empty by default`() {
        assertTrue(repository().records().isEmpty())
    }

    @Test
    fun `record appends preserving order`() {
        val repo = repository()
        val first = record(difficulty = AiDifficulty.EASY)
        val second = record(won = false, difficulty = AiDifficulty.HARD)
        repo.record(first)
        repo.record(second)
        assertEquals(listOf(first, second), repo.records())
    }

    @Test
    fun `records survive across repository instances sharing settings`() {
        val settings = MapSettings()
        repository(settings).record(record())
        assertEquals(1, repository(settings).records().size)
    }

    @Test
    fun `clear removes all records`() {
        val repo = repository()
        repo.record(record())
        repo.clear()
        assertTrue(repo.records().isEmpty())
    }

    @Test
    fun `corrupt data yields empty list instead of failing`() {
        val settings = MapSettings()
        SettingsStatisticsRepository(settings).record(record())
        // Overwrite with invalid JSON under the same key.
        settings.putString("game_records_v1", "not-json")
        assertTrue(repository(settings).records().isEmpty())
    }
}
