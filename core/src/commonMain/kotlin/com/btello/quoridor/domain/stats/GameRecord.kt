package com.btello.quoridor.domain.stats

import com.btello.quoridor.domain.ai.AiDifficulty
import kotlinx.serialization.Serializable

/**
 * Registro de una partida finalizada, visto desde la perspectiva del jugador
 * humano ([com.btello.quoridor.domain.model.PlayerId] 0).
 *
 * Es una entidad de dominio pura: no conoce la UI ni los detalles de
 * persistencia. La capa de datos la serializa para almacenarla.
 *
 * @property won `true` si el jugador humano ganó la partida.
 * @property difficulty dificultad de la IA, o `null` en partidas locales 1 vs 1.
 * @property durationMillis duración de la partida en milisegundos.
 * @property moveCount cantidad de jugadas realizadas por el ganador.
 * @property wallsUsed cantidad de muros colocados por el ganador.
 */
@Serializable
data class GameRecord(
    val won: Boolean,
    val difficulty: AiDifficulty?,
    val durationMillis: Long,
    val moveCount: Int,
    val wallsUsed: Int,
) {
    init {
        require(durationMillis >= 0) { "Duration must be non-negative" }
        require(moveCount >= 0) { "Move count must be non-negative" }
        require(wallsUsed >= 0) { "Walls used must be non-negative" }
    }
}
