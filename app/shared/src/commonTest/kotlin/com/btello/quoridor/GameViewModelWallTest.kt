package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameViewModelWallTest {

    private val config = GameConfig(playerCount = 2)

    private fun viewModel() = GameViewModel(setup = GameSetup(config), autoRunAi = false)

    @Test
    fun `a human turn can place walls`() {
        assertTrue(viewModel().uiState.canPlaceWall)
    }

    @Test
    fun `legal walls are exposed on a human turn`() {
        val legalWalls = viewModel().uiState.legalWalls
        // Tablero vacío 9x9: 8x8 posiciones por orientación.
        assertEquals(128, legalWalls.size)
    }

    @Test
    fun `placed and overlapping walls are no longer legal`() {
        val vm = viewModel()
        val wall = Wall(0, 0, WallOrientation.HORIZONTAL)
        vm.onEvent(GameEvent.WallDrop(wall))

        val legalWalls = vm.uiState.legalWalls
        assertFalse(wall in legalWalls)
        assertFalse(Wall(0, 1, WallOrientation.HORIZONTAL) in legalWalls)
        assertFalse(Wall(0, 0, WallOrientation.VERTICAL) in legalWalls)
    }

    @Test
    fun `ai turn exposes no legal walls`() {
        val vm = GameViewModel(
            setup = GameSetup(config, aiPlayers = setOf(PlayerId(1))),
            autoRunAi = false,
        )
        vm.onEvent(GameEvent.WallDrop(Wall(0, 0, WallOrientation.HORIZONTAL)))
        assertTrue(vm.uiState.legalWalls.isEmpty())
        assertFalse(vm.uiState.canPlaceWall)
    }

    @Test
    fun `dropping a legal wall places it and passes the turn`() {
        val vm = viewModel()
        val wall = Wall(0, 0, WallOrientation.HORIZONTAL)

        vm.onEvent(GameEvent.WallDrop(wall))

        assertTrue(wall in vm.uiState.gameState.board.walls)
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
    }

    @Test
    fun `dropping an overlapping wall is rejected without feedback`() {
        val vm = viewModel()
        vm.onEvent(GameEvent.WallDrop(Wall(0, 0, WallOrientation.HORIZONTAL)))

        val overlapping = Wall(0, 1, WallOrientation.HORIZONTAL)
        vm.onEvent(GameEvent.WallDrop(overlapping))

        assertFalse(overlapping in vm.uiState.gameState.board.walls)
        assertNull(vm.uiState.feedback)
        // Sigue siendo el turno del jugador 1: la jugada inválida no lo consume.
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
    }
}
