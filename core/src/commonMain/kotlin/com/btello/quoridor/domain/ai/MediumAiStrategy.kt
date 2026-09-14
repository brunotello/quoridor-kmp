package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random

/**
 * IA intermedia ([AiDifficulty.MEDIUM]): greedy por camino más corto.
 *
 * Avanza por el camino más corto propio y sólo coloca un muro cuando mejora
 * estrictamente la diferencia de distancias `dRival - dSelf` respecto a avanzar,
 * conservando muros en caso contrario. Los empates se resuelven al azar.
 */
internal class MediumAiStrategy(
    private val random: Random,
) : AiStrategy {

    override fun chooseMove(state: GameState, playerId: PlayerId): Move? {
        val moves = QuoridorRules.getLegalMoves(state)
        if (moves.isEmpty()) return null

        val pawnMoves = moves.filterIsInstance<Move.PawnMove>()
        val wallMoves = moves.filterIsInstance<Move.PlaceWall>()

        val bestPawn = pickBest(pawnMoves, random) { scoreAfter(state, it, playerId) }
        val bestWall = pickBest(wallMoves, random) { scoreAfter(state, it, playerId) }

        if (bestPawn == null) return bestWall
        if (bestWall == null) return bestPawn

        val pawnScore = scoreAfter(state, bestPawn, playerId)
        val wallScore = scoreAfter(state, bestWall, playerId)
        return if (wallScore > pawnScore) bestWall else bestPawn
    }
}
