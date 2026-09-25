package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlin.math.abs
import kotlin.random.Random

/** Distancia asignada cuando un jugador no tiene camino a su meta (no debería ocurrir en estados válidos). */
internal const val UNREACHABLE_DISTANCE = 1_000

/** Puntaje de una posición ganada/perdida, dominante frente a la heurística de distancias. */
internal const val TERMINAL_SCORE = 1_000.0

private const val SCORE_EPSILON = 1e-9

/** Peso del balance de muros disponibles en el puntaje posicional (coste de oportunidad de gastar un muro). */
internal const val WALL_BALANCE_WEIGHT = 1.0

/** Distancia del camino más corto de [id] a su meta; [UNREACHABLE_DISTANCE] si no hay camino. */
internal fun distanceToGoal(state: GameState, id: PlayerId): Int =
    QuoridorRules.shortestPathLength(state, id) ?: UNREACHABLE_DISTANCE

/** Identificadores de los rivales de [me] presentes en la partida. */
internal fun opponentsOf(state: GameState, me: PlayerId): List<PlayerId> =
    state.players.map { it.id }.filter { it != me }

private fun wallsOf(state: GameState, id: PlayerId): Int =
    state.players.firstOrNull { it.id == id }?.wallsRemaining ?: 0

/**
 * Puntaje posicional desde la perspectiva de [me]: diferencia entre la suma de
 * las distancias de **todos** los rivales a su meta y la propia, más el balance
 * de muros disponibles (para que gastar un muro tenga un coste de oportunidad).
 *
 * Sumar sobre todos los rivales (en lugar de mirar sólo al más cercano) hace que
 * un muro tenga valor también en partidas de 4 jugadores, donde bloquear a un
 * rival casi nunca cambia el mínimo. En partidas de 2 jugadores hay un único
 * rival, por lo que la suma equivale a la distancia de ese rival y el
 * comportamiento no cambia. Cuanto mayor, mejor para [me].
 */
internal fun positionalScore(state: GameState, me: PlayerId): Double {
    val self = distanceToGoal(state, me)
    val opponents = opponentsOf(state, me)
    val rivalDistanceSum = opponents.sumOf { distanceToGoal(state, it) }
    val nearestRival = opponents.minByOrNull { distanceToGoal(state, it) }
    val wallBalance = wallsOf(state, me) - (nearestRival?.let { wallsOf(state, it) } ?: 0)
    return (rivalDistanceSum - self).toDouble() + WALL_BALANCE_WEIGHT * wallBalance
}

/** Estado resultante de aplicar [move], o `null` si la jugada no es aplicable. */
internal fun stateAfter(state: GameState, move: Move): GameState? {
    val result = QuoridorRules.applyMove(state, move)
    return if (result.isSuccessful) result.state else null
}

/** Puntaje posicional para [me] tras aplicar [move]; muy negativo si la jugada no aplica. */
internal fun scoreAfter(state: GameState, move: Move, me: PlayerId): Double {
    val next = stateAfter(state, move) ?: return Double.NEGATIVE_INFINITY
    return positionalScore(next, me)
}

/**
 * Conjunto de jugadas a explorar para [id] en [state]: todos los movimientos de
 * peón más los [wallLimit] muros mejor puntuados a 1 ply. Acota el factor de
 * ramificación de los algoritmos de búsqueda descartando muros poco prometedores.
 */
internal fun candidateMoves(
    state: GameState,
    id: PlayerId,
    moves: List<Move>,
    wallLimit: Int,
): List<Move> {
    val pawnMoves = moves.filterIsInstance<Move.PawnMove>()
    val wallMoves = moves.filterIsInstance<Move.PlaceWall>()
        .sortedByDescending { scoreAfter(state, it, id) }
        .take(wallLimit)
    return pawnMoves + wallMoves
}

/**
 * Elige de [items] el de mayor puntaje según [score], resolviendo empates de
 * forma pseudoaleatoria con [random] (misma semilla ⇒ misma elección).
 */
internal inline fun <T> pickBest(items: List<T>, random: Random, score: (T) -> Double): T? {
    if (items.isEmpty()) return null
    val best = mutableListOf<T>()
    var bestScore = Double.NEGATIVE_INFINITY
    for (item in items) {
        val value = score(item)
        when {
            value > bestScore + SCORE_EPSILON -> {
                bestScore = value
                best.clear()
                best.add(item)
            }
            abs(value - bestScore) <= SCORE_EPSILON -> best.add(item)
        }
    }
    if (best.isEmpty()) return null
    return best[random.nextInt(best.size)]
}
