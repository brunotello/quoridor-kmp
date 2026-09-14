package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Turn
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuoridorAiTest {

    private fun freshState() = QuoridorRules.startGame(2)

    @Test
    fun `every difficulty returns a legal move on a fresh board`() {
        val state = freshState()
        val legal = QuoridorRules.getLegalMoves(state)
        for (difficulty in AiDifficulty.entries) {
            val move = chooseAiMove(state, PlayerId(0), difficulty, Random(7))
            assertTrue(move in legal, "$difficulty returned an illegal move: $move")
        }
    }

    @Test
    fun `same seed produces the same move`() {
        val state = freshState()
        for (difficulty in AiDifficulty.entries) {
            val first = chooseAiMove(state, PlayerId(0), difficulty, Random(123))
            val second = chooseAiMove(state, PlayerId(0), difficulty, Random(123))
            assertEquals(first, second, "$difficulty is not deterministic for a fixed seed")
        }
    }

    @Test
    fun `returns null when there are no legal moves`() {
        val state = freshState().copy(turn = Turn(PlayerId(9)))
        for (difficulty in AiDifficulty.entries) {
            assertNull(chooseAiMove(state, PlayerId(9), difficulty, Random(1)))
        }
    }

    @Test
    fun `never places a wall when no walls remain`() {
        val base = freshState()
        val noWalls = base.copy(players = base.players.map { it.copy(wallsRemaining = 0) })
        for (difficulty in AiDifficulty.entries) {
            val move = chooseAiMove(noWalls, PlayerId(0), difficulty, Random(3))
            assertTrue(move is Move.PawnMove, "$difficulty tried to place a wall with no walls left")
        }
    }

    @Test
    fun `medium advances along the shortest path on an open board`() {
        val state = freshState()
        val move = chooseAiMove(state, PlayerId(0), AiDifficulty.MEDIUM, Random(1))
        assertTrue(move is Move.PawnMove)
        assertEquals(Cell(1, 4), (move as Move.PawnMove).to)
    }

    @Test
    fun `hard takes the winning move when one step from the goal`() {
        val base = freshState()
        val nearGoal = base.copy(
            players = listOf(
                base.players[0].copy(position = Cell(7, 4)),
                base.players[1].copy(position = Cell(1, 0)),
            ),
        )
        val move = chooseAiMove(nearGoal, PlayerId(0), AiDifficulty.HARD, Random(1))
        assertTrue(move is Move.PawnMove)
        assertEquals(Cell(8, 4), (move as Move.PawnMove).to)
    }

    @Test
    fun `shortestPathLength measures distance to the goal side`() {
        val state = freshState()
        assertEquals(8, QuoridorRules.shortestPathLength(state, PlayerId(0)))
        assertEquals(8, QuoridorRules.shortestPathLength(state, PlayerId(1)))
    }

    @Test
    fun `shortestPathLength is zero once the goal is reached`() {
        val base = freshState()
        val atGoal = base.copy(players = listOf(base.players[0].copy(position = Cell(8, 3)), base.players[1]))
        assertEquals(0, QuoridorRules.shortestPathLength(atGoal, PlayerId(0)))
    }

    @Test
    fun `shortestPathLength returns null for an unknown player`() {
        assertNull(QuoridorRules.shortestPathLength(freshState(), PlayerId(42)))
    }

    @Test
    fun `applying an ai move keeps the game consistent`() {
        var state = freshState()
        val move = chooseAiMove(state, PlayerId(0), AiDifficulty.MEDIUM, Random(9))!!
        val result = QuoridorRules.applyMove(state, move)
        assertTrue(result.isSuccessful)
        state = result.state!!
        assertEquals(PlayerId(1), state.turn.playerId)
        assertTrue(state.status == GameStatus.IN_PROGRESS)
    }
}
