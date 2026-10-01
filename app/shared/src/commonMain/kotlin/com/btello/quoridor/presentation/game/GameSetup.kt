package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.CompetitiveConfig

/**
 * Configuración de una partida a iniciar desde el menú.
 *
 * Envuelve la [GameConfig] de dominio y añade la información propia de la capa
 * de presentación sobre qué jugadores controla la IA y con qué [AiDifficulty].
 * El dominio no conoce el concepto de "jugador IA".
 *
 * [competitive] lleva la configuración competitiva (rondas y tiempo por turno)
 * elegida para las partidas locales; por defecto es una única ronda sin
 * temporizador. En el modo online la gestiona la sala sincronizada.
 */
internal data class GameSetup(
    val config: GameConfig,
    val aiPlayers: Set<PlayerId> = emptySet(),
    val difficulty: AiDifficulty? = null,
    val competitive: CompetitiveConfig = CompetitiveConfig(),
    val online: OnlineSession? = null,
)
