package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId

/**
 * Configuración de una partida a iniciar desde el menú.
 *
 * Envuelve la [GameConfig] de dominio y añade la información propia de la capa
 * de presentación sobre qué jugadores controla la IA y con qué [AiDifficulty].
 * El dominio no conoce el concepto de "jugador IA".
 */
internal data class GameSetup(
    val config: GameConfig,
    val aiPlayers: Set<PlayerId> = emptySet(),
    val difficulty: AiDifficulty? = null,
)
