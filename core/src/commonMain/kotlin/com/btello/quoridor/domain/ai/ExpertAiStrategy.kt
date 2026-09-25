package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random

/**
 * IA experta ([AiDifficulty.EXPERT]): minimax con poda alfa-beta a [maxDepth] plies.
 *
 * A diferencia de [HardAiStrategy] (2 plies), profundiza varias jugadas por bando
 * alternando maximización (la IA) y minimización (el rival), lo que le permite
 * anticipar combinaciones de muros y trampas a varias jugadas vista. La poda
 * alfa-beta y el recorte a los muros más prometesores ([ownWallCandidates] /
 * [opponentWallCandidates]) mantienen el coste acotado.
 */
internal class ExpertAiStrategy(
    private val random: Random,
    private val maxDepth: Int = 3,
    private val ownWallCandidates: Int = 10,
    private val opponentWallCandidates: Int = 8,
) : AiStrategy {

    override fun chooseMove(state: GameState, playerId: PlayerId): Move? {
        val candidates = rootCandidates(state, playerId) ?: return null
        return pickBest(candidates, random) { move -> rootScore(state, playerId, move) }
    }

    /**
     * Variante paralela: evalúa cada jugada candidata de la raíz en su propia
     * corrutina. Como en la raíz cada rama usa una ventana alfa-beta completa e
     * independiente, repartirlas no altera el resultado (misma semilla ⇒ misma
     * jugada) pero aprovecha varios núcleos para responder más rápido.
     */
    override suspend fun chooseMoveAsync(state: GameState, playerId: PlayerId): Move? = coroutineScope {
        val candidates = rootCandidates(state, playerId) ?: return@coroutineScope null
        val scored = candidates
            .map { move -> async { move to rootScore(state, playerId, move) } }
            .awaitAll()
        pickBest(scored, random) { it.second }?.first
    }

    /** Jugadas candidatas de la raíz para [playerId], o `null` si no hay jugadas legales. */
    private fun rootCandidates(state: GameState, playerId: PlayerId): List<Move>? {
        val moves = QuoridorRules.getLegalMoves(state)
        if (moves.isEmpty()) return null
        return candidateMoves(state, playerId, moves, ownWallCandidates)
    }

    /** Puntaje minimax de aplicar [move] en la raíz desde la perspectiva de [me]. */
    private fun rootScore(state: GameState, me: PlayerId, move: Move): Double {
        val next = stateAfter(state, move) ?: return Double.NEGATIVE_INFINITY
        return minimax(next, me, maxDepth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    }

    /**
     * Puntaje minimax de [state] desde la perspectiva de [me] con poda alfa-beta.
     * Alterna entre maximizar (turno de [me]) y minimizar (turno del rival) hasta
     * agotar [depth] o alcanzar un estado terminal.
     */
    private fun minimax(
        state: GameState,
        me: PlayerId,
        depth: Int,
        alpha: Double,
        beta: Double,
    ): Double {
        if (QuoridorRules.isGameOver(state)) {
            return if (state.winner == me) TERMINAL_SCORE else -TERMINAL_SCORE
        }
        if (depth <= 0) return positionalScore(state, me)

        val toMove = state.turn.playerId
        val moves = QuoridorRules.getLegalMoves(state)
        if (moves.isEmpty()) return positionalScore(state, me)

        val maximizing = toMove == me
        val wallLimit = if (maximizing) ownWallCandidates else opponentWallCandidates
        val candidates = candidateMoves(state, toMove, moves, wallLimit)

        var a = alpha
        var b = beta
        if (maximizing) {
            var best = Double.NEGATIVE_INFINITY
            for (move in candidates) {
                val next = stateAfter(state, move) ?: continue
                val score = minimax(next, me, depth - 1, a, b)
                if (score > best) best = score
                if (best > a) a = best
                if (a >= b) break
            }
            return if (best == Double.NEGATIVE_INFINITY) positionalScore(state, me) else best
        }

        var best = Double.POSITIVE_INFINITY
        for (move in candidates) {
            val next = stateAfter(state, move) ?: continue
            val score = minimax(next, me, depth - 1, a, b)
            if (score < best) best = score
            if (best < b) b = best
            if (a >= b) break
        }
        return if (best == Double.POSITIVE_INFINITY) positionalScore(state, me) else best
    }
}
