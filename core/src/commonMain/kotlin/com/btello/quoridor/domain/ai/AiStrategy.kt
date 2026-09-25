package com.btello.quoridor.domain.ai

import com.btello.quoridor.domain.ai.AiDifficulty.EASY
import com.btello.quoridor.domain.ai.AiDifficulty.EXPERT
import com.btello.quoridor.domain.ai.AiDifficulty.HARD
import com.btello.quoridor.domain.ai.AiDifficulty.MEDIUM
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import kotlin.random.Random

/**
 * Estrategia de decisión de la IA: dada una partida en curso, elige una jugada
 * legal para el jugador indicado.
 *
 * Es una función pura del `(state, playerId, random)`: no guarda estado oculto y
 * no depende de la UI ni de corrutinas. Toda jugada devuelta es legal según
 * [com.btello.quoridor.domain.rules.QuoridorRules].
 */
interface AiStrategy {

    /**
     * Devuelve una jugada legal para [playerId] en [state], o `null` si no hay
     * ninguna (partida terminada o jugador sin movimientos).
     */
    fun chooseMove(state: GameState, playerId: PlayerId): Move?

    /**
     * Variante suspendible de [chooseMove]. Las estrategias con búsqueda costosa
     * pueden sobrescribirla para repartir el trabajo entre varios hilos usando
     * corrutinas. Por defecto delega en [chooseMove] (cómputo secuencial).
     */
    suspend fun chooseMoveAsync(state: GameState, playerId: PlayerId): Move? =
        chooseMove(state, playerId)

    companion object {
        /** Crea la estrategia asociada al nivel [difficulty]. */
        fun forDifficulty(
            difficulty: AiDifficulty,
            random: Random = Random.Default,
        ): AiStrategy = when (difficulty) {
            EASY -> EasyAiStrategy(random)
            MEDIUM -> MediumAiStrategy(random)
            HARD -> HardAiStrategy(random)
            EXPERT -> ExpertAiStrategy(random)
        }
    }
}
