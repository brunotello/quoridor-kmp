package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.presentation.stats.StatisticsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class StatisticsViewModelTest {

    private fun record(won: Boolean = true) =
        GameRecord(won, AiDifficulty.EASY, durationMillis = 1_000, moveCount = 10, wallsUsed = 2)

    @Test
    fun `loads aggregated statistics on creation`() {
        val repo = FakeStatisticsRepository(listOf(record(), record(won = false)))
        val vm = StatisticsViewModel(repo)
        assertEquals(2, vm.uiState.statistics.totalGames)
    }

    @Test
    fun `empty repository yields empty statistics`() {
        val vm = StatisticsViewModel(FakeStatisticsRepository())
        assertEquals(0, vm.uiState.statistics.totalGames)
    }

    @Test
    fun `refresh picks up records added after creation`() {
        val repo = FakeStatisticsRepository()
        val vm = StatisticsViewModel(repo)
        assertEquals(0, vm.uiState.statistics.totalGames)

        repo.record(record())
        vm.refresh()

        assertEquals(1, vm.uiState.statistics.totalGames)
    }
}
