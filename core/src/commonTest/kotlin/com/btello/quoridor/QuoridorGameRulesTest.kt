package com.btello.quoridor

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Turn
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.domain.rules.DomainError
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random
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
    fun `pawn cannot jump over an opponent through a wall between them`() {
        val base = QuoridorRules.startGame(2)
        // P0 en (4,4), oponente P1 justo debajo en (5,4). Muro horizontal en (4,4)
        // cubre las columnas 4 y 5 entre las filas 4 y 5, separando a ambos peones.
        val state = base.copy(
            board = base.board.copy(walls = setOf(Wall(4, 4, WallOrientation.HORIZONTAL))),
            players = listOf(
                base.players[0].copy(position = Cell(4, 4)),
                base.players[1].copy(position = Cell(5, 4)),
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        val straightJump = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(6, 4)),
        )
        val diagonalJump = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(5, 5)),
        )

        assertFalse(straightJump.isValid)
        assertFalse(diagonalJump.isValid)
    }

    @Test
    fun `pawn jumps straight over an opponent when the path is clear`() {
        val base = QuoridorRules.startGame(2)
        val state = base.copy(
            players = listOf(
                base.players[0].copy(position = Cell(4, 4)),
                base.players[1].copy(position = Cell(5, 4)),
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        val jump = QuoridorRules.applyMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(6, 4)),
        )

        assertTrue(jump.isSuccessful)
        assertEquals(Cell(6, 4), jump.state!!.players.first { it.id == PlayerId(0) }.position)
    }

    @Test
    fun `pawn jumps diagonally when a wall is behind the opponent`() {
        val base = QuoridorRules.startGame(2)
        // Oponente en (5,4) con un muro horizontal en (5,4) detrás (entre filas 5 y 6).
        val state = base.copy(
            board = base.board.copy(walls = setOf(Wall(5, 4, WallOrientation.HORIZONTAL))),
            players = listOf(
                base.players[0].copy(position = Cell(4, 4)),
                base.players[1].copy(position = Cell(5, 4)),
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        val straightBlocked = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(6, 4)),
        )
        val diagonalLeft = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(5, 3)),
        )
        val diagonalRight = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(5, 5)),
        )

        assertFalse(straightBlocked.isValid)
        assertTrue(diagonalLeft.isValid)
        assertTrue(diagonalRight.isValid)
    }

    @Test
    fun `pawn cannot reach cells that are not valid diagonal jumps`() {
        val base = QuoridorRules.startGame(2)
        val state = base.copy(
            board = base.board.copy(walls = setOf(Wall(5, 4, WallOrientation.HORIZONTAL))),
            players = listOf(
                base.players[0].copy(position = Cell(4, 4)),
                base.players[1].copy(position = Cell(5, 4)),
            ),
            turn = com.btello.quoridor.domain.model.Turn(PlayerId(0)),
        )

        // (6,5) sería el resultado del offset diagonal erróneo aplicado sobre el oponente.
        val bogus = QuoridorRules.validateMove(
            state,
            Move.PawnMove(PlayerId(0), Cell(4, 4), Cell(6, 5)),
        )

        assertFalse(bogus.isValid)
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

    @Test
    fun `skipping a turn passes it to the next player without changing the board`() {
        val state = QuoridorRules.startGame(2)

        val result = QuoridorRules.skipTurn(state)

        assertEquals(PlayerId(1), result.turn.playerId)
        assertEquals(state.players, result.players)
        assertEquals(state.board, result.board)
        assertEquals(GameStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun `skipping the last player's turn wraps to the first`() {
        val state = QuoridorRules.startGame(4).copy(turn = Turn(PlayerId(3)))

        val result = QuoridorRules.skipTurn(state)

        assertEquals(PlayerId(0), result.turn.playerId)
    }

    @Test
    fun `skipping a turn after a player left follows the remaining order`() {
        val state = QuoridorRules.withPlayerRemoved(QuoridorRules.startGame(4), PlayerId(2))
            .copy(turn = Turn(PlayerId(1)))

        val result = QuoridorRules.skipTurn(state)

        assertEquals(PlayerId(3), result.turn.playerId)
    }

    @Test
    fun `skipping a turn on a finished game keeps the state`() {
        val state = QuoridorRules.startGame(2).copy(status = GameStatus.GAME_OVER, winner = PlayerId(0))

        assertEquals(state, QuoridorRules.skipTurn(state))
    }

    @Test
    fun `removing a player from a 1v1 ends the game and the rival wins`() {
        val state = QuoridorRules.startGame(2)

        val result = QuoridorRules.withPlayerRemoved(state, PlayerId(0))

        assertEquals(1, result.players.size)
        assertEquals(GameStatus.GAME_OVER, result.status)
        assertEquals(PlayerId(1), result.winner)
        assertEquals(PlayerId(1), result.turn.playerId)
    }

    @Test
    fun `removing the current player advances the turn to the next player`() {
        val state = QuoridorRules.startGame(4)

        val result = QuoridorRules.withPlayerRemoved(state, PlayerId(0))

        assertEquals(3, result.players.size)
        assertEquals(GameStatus.IN_PROGRESS, result.status)
        assertEquals(PlayerId(1), result.turn.playerId)
        assertFalse(result.players.any { it.id == PlayerId(0) })
    }

    @Test
    fun `removing the last player in turn order wraps the turn to the first`() {
        val state = QuoridorRules.startGame(4).let {
            it.copy(turn = com.btello.quoridor.domain.model.Turn(PlayerId(3)))
        }

        val result = QuoridorRules.withPlayerRemoved(state, PlayerId(3))

        assertEquals(3, result.players.size)
        assertEquals(PlayerId(0), result.turn.playerId)
    }

    @Test
    fun `removing a player who is not on turn keeps the current turn`() {
        val state = QuoridorRules.startGame(4)

        val result = QuoridorRules.withPlayerRemoved(state, PlayerId(2))

        assertEquals(3, result.players.size)
        assertEquals(PlayerId(0), result.turn.playerId)
    }

    @Test
    fun `removing an unknown player leaves the state unchanged`() {
        val state = QuoridorRules.startGame(2)

        val result = QuoridorRules.withPlayerRemoved(state, PlayerId(5))

        assertEquals(state, result)
    }

    @Test
    fun `removing a player from a finished game leaves the state unchanged`() {
        val finished = QuoridorRules.startGame(2).copy(status = GameStatus.GAME_OVER, winner = PlayerId(0))

        val result = QuoridorRules.withPlayerRemoved(finished, PlayerId(1))

        assertEquals(finished, result)
    }

    @Test
    fun `a random starting player can be someone other than the first`() {
        val state = QuoridorRules.startGame(4)

        // Seed elegido para que el jugador inicial no sea PlayerId(0).
        val starters = (0 until 20)
            .map { seed -> QuoridorRules.withRandomStartingPlayer(state, Random(seed)).turn.playerId }
            .toSet()

        assertTrue(starters.any { it != PlayerId(0) })
        assertTrue(starters.all { id -> state.players.any { it.id == id } })
    }

    @Test
    fun `a random starting player only changes the turn`() {
        val state = QuoridorRules.startGame(4)

        val result = QuoridorRules.withRandomStartingPlayer(state, Random(1))

        assertEquals(state.players, result.players)
        assertEquals(state.board, result.board)
        assertEquals(state.copy(turn = result.turn), result)
    }

    @Test
    fun `a random starting player on an empty game leaves the state unchanged`() {
        val empty = QuoridorRules.startGame(2).copy(players = emptyList())

        val result = QuoridorRules.withRandomStartingPlayer(empty, Random(0))

        assertEquals(empty, result)
    }
}
