package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random

/**
 * IA avanzada ([AiDifficulty.HARD]): minimax acotado a 2 plies.
 *
 * Por cada jugada propia candidata evalúa la mejor respuesta del rival (la que
 * más perjudica a la IA) y elige la jugada cuyo peor caso maximiza el puntaje
 * posicional. Para acotar el coste, sólo considera los muros más prometedores
 * (mejor `dRival - dSelf` a 1 ply) tanto propios como del rival.
 */
internal class HardAiStrategy(
    private val random: Random,
    private val ownWallCandidates: Int = 8,
    private val opponentWallCandidates: Int = 6,
) : AiStrategy {

    override fun chooseMove(state: GameState, playerId: PlayerId): Move? {
        val moves = QuoridorRules.getLegalMoves(state)
        if (moves.isEmpty()) return null

        val candidates = candidateMoves(state, playerId, moves, ownWallCandidates)
        return pickBest(candidates, random) { move ->
            val next = stateAfter(state, move) ?: return@pickBest Double.NEGATIVE_INFINITY
            if (QuoridorRules.isGameOver(next)) TERMINAL_SCORE else opponentBestReply(next, playerId)
        }
    }

    /** Peor puntaje posicional para [me] tras la mejor respuesta del rival en [state]. */
    private fun opponentBestReply(state: GameState, me: PlayerId): Double {
        val opponentId = state.turn.playerId
        val opponentMoves = QuoridorRules.getLegalMoves(state)
        if (opponentMoves.isEmpty()) return positionalScore(state, me)

        val candidates = candidateMoves(state, opponentId, opponentMoves, opponentWallCandidates)
        var worst = Double.POSITIVE_INFINITY
        for (move in candidates) {
            val next = stateAfter(state, move) ?: continue
            val score = if (QuoridorRules.isGameOver(next)) -TERMINAL_SCORE else positionalScore(next, me)
            if (score < worst) worst = score
        }
        return if (worst == Double.POSITIVE_INFINITY) positionalScore(state, me) else worst
    }
}
