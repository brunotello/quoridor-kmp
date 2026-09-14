package com.btello.quoridor

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.domain.rules.DomainError
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoridorGameRulesTest {
    @Test
    fun `startGame creates valid two-player state`() {
        val state = QuoridorRules.startGame(GameConfig(playerCount = 2))
        assertEquals(2, state.players.size)
        assertEquals(PlayerId(0), state.turn.playerId)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(10, state.players.first().wallsRemaining)
    }

    @Test
    fun `player can move to adjacent cell`() {
        val state = QuoridorRules.startGame(2)
        val move = Move.PawnMove(PlayerId(0), Cell(0, 4), Cell(1, 4))

        val result = QuoridorRules.applyMove(state, move)

        assertTrue(result.isSuccessful)
        assertEquals(Cell(1, 4), result.state!!.players.first { it.id == PlayerId(0) }.position)
        assertEquals(PlayerId(1), result.state.turn.playerId)
    }

    @Test
    fun `wall can be placed while preserving at least one path`() {
        val state = QuoridorRules.startGame(2)
        val wall = Wall(1, 3, WallOrientation.HORIZONTAL)
        val move = Move.PlaceWall(PlayerId(0), wall)

        val result = QuoridorRules.validateMove(state, move)

        assertTrue(result.isValid)
    }

    @Test
    fun `wall occupancy decreases remaining walls`() {
        val state = QuoridorRules.startGame(2)
        val wall = Wall(1, 3, WallOrientation.HORIZONTAL)
        val validMove = Move.PlaceWall(PlayerId(0), wall)

        val result = QuoridorRules.applyMove(state, validMove)

        assertTrue(result.isSuccessful)
        assertEquals(9, result.state!!.players.first { it.id == PlayerId(0) }.wallsRemaining)
    }

    @Test
    fun `adjacent parallel wall overlapping a segment is rejected`() {
        val state = QuoridorRules.startGame(2)
        val placed = QuoridorRules.applyMove(
            state,
            Move.PlaceWall(PlayerId(0), Wall(1, 2, WallOrientation.HORIZONTAL)),
        )
        assertTrue(placed.isSuccessful)

        // Wall at (1,1) spans cols 1..2, overlapping the segment at col 2 of the wall at (1,2).
        val overlapping = QuoridorRules.validateMove(
            placed.state!!.copy(turn = com.btello.quoridor.domain.model.Turn(PlayerId(1))),
            Move.PlaceWall(PlayerId(1), Wall(1, 1, WallOrientation.HORIZONTAL)),
        )

        assertFalse(overlapping.isValid)
        assertTrue(overlapping.error is DomainError.WallOverlap)
    }

    @Test
    fun `horizontal wall blocks both of its covered columns`() {
        val base = QuoridorRules.startGame(2)
        // Horizontal wall at (0,4) covers columns 4 and 5 between rows 0 and 1.
        val state = base.copy(
            board = base.board.copy(walls = setOf(Wall(0, 4, WallOrientation.HORIZONTAL))),
            players = listOf(
                base.players[0].copy(position = Cell(0, 5)),
                base.players[1],
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        val blocked = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(0, 5), Cell(1, 5)),
        )

        assertFalse(blocked.isValid)
    }

    @Test
    fun `game is over when player reaches opposite border`() {
        val state = QuoridorRules.startGame(2)
        val nearGoal = state.copy(
            players = listOf(
                state.players[0].copy(position = Cell(7, 4)),
                state.players[1].copy(position = Cell(0, 4)),
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        val result = QuoridorRules.applyMove(nearGoal, Move.PawnMove(PlayerId(0), Cell(7, 4), Cell(8, 4)))

        assertTrue(result.isSuccessful)
        assertEquals(GameStatus.GAME_OVER, result.state!!.status)
    }
}
