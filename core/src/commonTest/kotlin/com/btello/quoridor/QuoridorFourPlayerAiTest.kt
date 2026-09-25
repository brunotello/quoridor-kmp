package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.ai.positionalScore
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifica que el motor de IA funcione en partidas de 4 jugadores: cada nivel
 * elige siempre una jugada legal para el jugador en turno y una partida con los 4
 * peones controlados por IA avanza hasta terminar sin jugadas ilegales.
 */
class QuoridorFourPlayerAiTest {

    @Test
    fun `every difficulty returns a legal move for each player in a four player game`() {
        for (difficulty in AiDifficulty.entries) {
            var state = QuoridorRules.startGame(GameConfig(playerCount = 4))
            repeat(4) {
                val playerId = state.turn.playerId
                val move = chooseAiMove(state, playerId, difficulty, Random(7))
                assertNotNull(move, "$difficulty should produce a move for $playerId")
                assertTrue(
                    move in QuoridorRules.getLegalMoves(state),
                    "$difficulty produced an illegal move for $playerId",
                )
                assertEquals(playerId, move.playerId)
                state = QuoridorRules.applyMove(state, move).state ?: state
            }
        }
    }

    @Test
    fun `a full four player ai game reaches a winner without illegal moves`() {
        var state = QuoridorRules.startGame(GameConfig(playerCount = 4))
        var guard = 0
        while (state.status == GameStatus.IN_PROGRESS && guard < 2_000) {
            val playerId = state.turn.playerId
            val move = chooseAiMove(state, playerId, AiDifficulty.MEDIUM, Random(guard.toLong()))
            assertNotNull(move)
            val result = QuoridorRules.applyMove(state, move)
            assertTrue(result.isSuccessful, "AI produced an illegal move: $move")
            state = result.state ?: state
            guard++
        }
        assertEquals(GameStatus.GAME_OVER, state.status)
        assertNotNull(state.winner)
        assertFalse(guard >= 2_000, "Game did not terminate in a reasonable number of turns")
    }

    @Test
    fun `positional score aggregates every rival distance, not just the nearest`() {
        // Tablero inicial simétrico de 4 jugadores: cada peón está a distancia 8 de
        // su meta. La suma de los 3 rivales (24) menos la propia (8) da 16. Con la
        // lógica anterior basada sólo en el rival más cercano el puntaje sería 0,
        // por lo que un muro nunca compensaba: este test bloquea esa regresión.
        val state = QuoridorRules.startGame(GameConfig(playerCount = 4))
        assertEquals(16.0, positionalScore(state, PlayerId(0)))
    }

    @Test
    fun `the ai actually places walls during a four player game`() {
        var state = QuoridorRules.startGame(GameConfig(playerCount = 4))
        var wallsPlaced = 0
        var guard = 0
        while (state.status == GameStatus.IN_PROGRESS && guard < 120) {
            val playerId = state.turn.playerId
            val move = chooseAiMove(state, playerId, AiDifficulty.HARD, Random(guard.toLong())) ?: break
            if (move is Move.PlaceWall) wallsPlaced++
            state = QuoridorRules.applyMove(state, move).state ?: break
            guard++
        }
        assertTrue(wallsPlaced > 0, "AI should place walls in a 4-player game, but placed none")
    }
}
