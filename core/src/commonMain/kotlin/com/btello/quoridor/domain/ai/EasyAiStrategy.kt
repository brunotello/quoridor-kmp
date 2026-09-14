package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.random.Random

/**
 * IA principiante ([AiDifficulty.EASY]).
 *
 * La mayor parte del tiempo avanza por el camino más corto, pero con cierta
 * probabilidad juega una jugada legal al azar (incluido colocar muros sin
 * criterio), lo que la hace subóptima y fácil de vencer.
 */
internal class EasyAiStrategy(
    private val random: Random,
    private val randomMoveChance: Double = 0.25,
) : AiStrategy {

    override fun chooseMove(state: GameState, playerId: PlayerId): Move? {
        val moves = QuoridorRules.getLegalMoves(state)
        if (moves.isEmpty()) return null

        val pawnMoves = moves.filterIsInstance<Move.PawnMove>()
        if (pawnMoves.isEmpty() || random.nextDouble() < randomMoveChance) {
            return moves[random.nextInt(moves.size)]
        }

        return pickBest(pawnMoves, random) { move ->
            val next = stateAfter(state, move) ?: return@pickBest Double.NEGATIVE_INFINITY
            -distanceToGoal(next, playerId).toDouble()
        }
    }
}
