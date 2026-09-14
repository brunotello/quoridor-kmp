package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.ai.AiStrategy
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Estrategia determinista: siempre juega el primer movimiento de peón legal. */
private class FirstPawnMoveStrategy : AiStrategy {
    override fun chooseMove(state: GameState, playerId: PlayerId): Move? =
        QuoridorRules.getLegalMoves(state).filterIsInstance<Move.PawnMove>().firstOrNull()
}

class GameViewModelAiTest {

    private fun aiSetup() = GameSetup(
        config = GameConfig(playerCount = 2),
        aiPlayers = setOf(PlayerId(1)),
        difficulty = AiDifficulty.EASY,
    )

    private fun aiViewModel() = GameViewModel(
        setup = aiSetup(),
        aiStrategy = FirstPawnMoveStrategy(),
        autoRunAi = false,
    )

    @Test
    fun `human moves first and the ai does not control the opening turn`() {
        val vm = aiViewModel()
        assertFalse(vm.aiControlsCurrentTurn())
        assertEquals(PlayerId(0), vm.uiState.gameState.turn.playerId)
        assertTrue(vm.uiState.legalTargets.isNotEmpty())
    }

    @Test
    fun `exposes the ai player so the ui can label it`() {
        val vm = aiViewModel()
        assertEquals(setOf(PlayerId(1)), vm.uiState.aiPlayers)
    }

    @Test
    fun `after the human moves it becomes the ai turn and input is blocked`() {
        val vm = aiViewModel()
        vm.onEvent(GameEvent.CellClick(Cell(1, 4)))

        assertTrue(vm.aiControlsCurrentTurn())
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
        assertTrue(vm.uiState.legalTargets.isEmpty(), "AI turn should not highlight targets")

        val before = vm.uiState.gameState
        vm.onEvent(GameEvent.CellClick(Cell(7, 4)))
        assertEquals(before, vm.uiState.gameState, "Human input must be ignored during the AI turn")
    }

    @Test
    fun `running the ai turn advances the ai and returns control to the human`() {
        val vm = aiViewModel()
        vm.onEvent(GameEvent.CellClick(Cell(1, 4)))
        val aiBefore = vm.uiState.gameState.players.first { it.id == PlayerId(1) }.position

        vm.runAiTurnsForTest()

        val aiAfter = vm.uiState.gameState.players.first { it.id == PlayerId(1) }.position
        assertTrue(aiAfter != aiBefore, "AI pawn should have moved")
        assertEquals(PlayerId(0), vm.uiState.gameState.turn.playerId)
        assertFalse(vm.aiControlsCurrentTurn())
        assertTrue(vm.uiState.legalTargets.isNotEmpty())
    }

    @Test
    fun `local games never hand control to the ai`() {
        val vm = GameViewModel(
            setup = GameSetup(config = GameConfig(playerCount = 2)),
            autoRunAi = false,
        )
        vm.onEvent(GameEvent.CellClick(Cell(1, 4)))
        assertFalse(vm.aiControlsCurrentTurn())
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
        assertTrue(vm.uiState.legalTargets.isNotEmpty())
    }
}
