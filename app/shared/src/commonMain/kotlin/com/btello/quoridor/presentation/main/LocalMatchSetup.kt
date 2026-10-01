package com.btello.quoridor.presentation.main

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI

/** Cantidades de jugadores ofrecidas al configurar una partida contra la IA. */
internal val LOCAL_AI_PLAYER_COUNT_OPTIONS: List<Int> = listOf(2, 4)

/**
 * Estado de configuración de una partida local, previo a arrancarla.
 *
 * Según el [mode], distintas secciones son relevantes: contra la IA se elige la
 * cantidad de jugadores (2 ó 4, siempre 1 humano contra el resto de IA) y la
 * dificultad, pero no el temporizador (no tiene sentido). En las partidas entre
 * humanos (1 vs 1 y 4 jugadores) se elige la cantidad de rondas y el tiempo por
 * turno. La cantidad de rondas se ofrece en todos los modos.
 */
internal data class LocalMatchSetupState(
    val mode: GameMode,
    val playerCount: Int = mode.playerCount,
    val difficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val turnTimeSeconds: Int? = null,
) {
    /** Sólo el modo contra la IA ofrece elegir la cantidad de jugadores (2 ó 4). */
    val showPlayerCount: Boolean get() = mode == VERSUS_AI

    /** Sólo el modo contra la IA ofrece elegir la dificultad. */
    val showDifficulty: Boolean get() = mode == VERSUS_AI

    /** El temporizador por turno se ofrece sólo en las partidas entre humanos. */
    val showTurnTime: Boolean get() = mode != VERSUS_AI

    /**
     * Setup de partida resultante. Contra la IA el humano es el jugador 0 y la IA
     * controla el resto; entre humanos no hay IA. El temporizador se descarta
     * cuando el modo no lo ofrece (contra la IA).
     */
    fun toGameSetup(): GameSetup {
        val aiCount = if (mode == VERSUS_AI) playerCount - 1 else 0
        val aiPlayers = aiPlayersForCount(playerCount, aiCount)
        return GameSetup(
            config = GameConfig(playerCount = playerCount),
            aiPlayers = aiPlayers,
            difficulty = if (aiPlayers.isNotEmpty()) difficulty else null,
            competitive = CompetitiveConfig(
                format = format,
                turnTimeSeconds = if (showTurnTime) turnTimeSeconds else null,
            ),
        )
    }
}
