package com.btello.quoridor

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameViewModelStatisticsTest {

    private fun localViewModel(repo: FakeStatisticsRepository) = GameViewModel(
        setup = GameSetup(config = GameConfig(playerCount = 2)),
        autoRunAi = false,
        statisticsRepository = repo,
    )

    /**
     * Lleva a Player 0 hasta la fila 8 (su meta) por la columna 3, mientras
     * Player 1 oscila en la fila 8 sin acercarse a su meta. Termina con victoria
     * de Player 0 en 9 jugadas y sin muros.
     */
    private fun playPlayerZeroWin(vm: GameViewModel) {
        val p0 = listOf(
            Cell(0, 3), Cell(1, 3), Cell(2, 3), Cell(3, 3), Cell(4, 3),
            Cell(5, 3), Cell(6, 3), Cell(7, 3), Cell(8, 3),
        )
        val p1 = listOf(
            Cell(8, 5), Cell(8, 4), Cell(8, 5), Cell(8, 4),
            Cell(8, 5), Cell(8, 4), Cell(8, 5), Cell(8, 4),
        )
        for (i in p0.indices) {
            vm.onEvent(GameEvent.CellClick(p0[i]))
            if (i < p1.size) vm.onEvent(GameEvent.CellClick(p1[i]))
        }
    }

    @Test
    fun `records a won local game with the winner move count`() {
        val repo = FakeStatisticsRepository()
        val vm = localViewModel(repo)

        playPlayerZeroWin(vm)

        assertTrue(vm.uiState.isGameOver)
        assertEquals(PlayerId(0), vm.uiState.gameState.winner)
        val records = repo.records()
        assertEquals(1, records.size)
        val record = records.single()
        assertTrue(record.won)
        assertEquals(null, record.difficulty)
        assertEquals(9, record.moveCount)
        assertEquals(0, record.wallsUsed)
    }

    @Test
    fun `records the game only once`() {
        val repo = FakeStatisticsRepository()
        val vm = localViewModel(repo)

        playPlayerZeroWin(vm)
        // Further input after game over must not add more records.
        vm.onEvent(GameEvent.CellClick(Cell(7, 3)))

        assertEquals(1, repo.records().size)
    }

    @Test
    fun `does not record while the game is still in progress`() {
        val repo = FakeStatisticsRepository()
        val vm = localViewModel(repo)

        vm.onEvent(GameEvent.CellClick(Cell(1, 4)))

        assertTrue(repo.records().isEmpty())
    }
}
