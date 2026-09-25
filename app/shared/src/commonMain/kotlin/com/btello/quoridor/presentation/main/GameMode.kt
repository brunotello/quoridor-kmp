package com.btello.quoridor.presentation.main

import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.main.GameMode.ONLINE
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.game_mode_4p
import quoridor.app.shared.generated.resources.game_mode_4p_description
import quoridor.app.shared.generated.resources.game_mode_4p_emoji
import quoridor.app.shared.generated.resources.game_mode_online
import quoridor.app.shared.generated.resources.game_mode_online_description
import quoridor.app.shared.generated.resources.game_mode_online_emoji
import quoridor.app.shared.generated.resources.game_mode_pvai
import quoridor.app.shared.generated.resources.game_mode_pvai_description
import quoridor.app.shared.generated.resources.game_mode_pvai_emoji
import quoridor.app.shared.generated.resources.game_mode_pvp
import quoridor.app.shared.generated.resources.game_mode_pvp_description
import quoridor.app.shared.generated.resources.game_mode_pvp_emoji

/**
 * Modos de juego ofrecidos en la pantalla "Nueva partida".
 *
 * Cada modo declara su título, descripción, emoji, si está disponible, la
 * cantidad de jugadores con la que arranca la partida, cuántos de ellos controla
 * la IA por defecto ([aiCount]) y si esa cantidad se elige en un paso previo
 * ([configurableAi]). El humano principal siempre es [PlayerId] 0; la IA controla
 * los últimos [aiCount] jugadores. Agregar un modo sólo requiere una entrada aquí
 * (más sus strings).
 */
internal enum class GameMode(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val emojiRes: StringResource,
    val enabled: Boolean,
    val playerCount: Int,
    val aiCount: Int,
    val configurableAi: Boolean = false,
) {
    VERSUS_AI(
        titleRes = Res.string.game_mode_pvai,
        descriptionRes = Res.string.game_mode_pvai_description,
        emojiRes = Res.string.game_mode_pvai_emoji,
        enabled = true,
        playerCount = 2,
        aiCount = 1,
    ),
    LOCAL_1V1(
        titleRes = Res.string.game_mode_pvp,
        descriptionRes = Res.string.game_mode_pvp_description,
        emojiRes = Res.string.game_mode_pvp_emoji,
        enabled = true,
        playerCount = 2,
        aiCount = 0,
    ),
    ONLINE(
        titleRes = Res.string.game_mode_online,
        descriptionRes = Res.string.game_mode_online_description,
        emojiRes = Res.string.game_mode_online_emoji,
        enabled = OnlinePlatform.isSupported,
        playerCount = 2,
        aiCount = 0,
    ),
    FOUR_PLAYERS(
        titleRes = Res.string.game_mode_4p,
        descriptionRes = Res.string.game_mode_4p_description,
        emojiRes = Res.string.game_mode_4p_emoji,
        enabled = true,
        playerCount = 4,
        aiCount = 0,
        configurableAi = true,
    ),
}

/**
 * Al elegir un modo con cantidad de IA ya fijada (> 0) primero se elige la
 * dificultad; los modos sin IA arrancan la partida directamente y los modos con
 * IA configurable primero eligen la cantidad de rivales.
 */
internal val GameMode.requiresDifficulty: Boolean
    get() = aiCount > 0 && !configurableAi

/** El modo online abre primero el lobby (crear/unirse a una sala) en vez de arrancar. */
internal val GameMode.requiresLobby: Boolean
    get() = this == ONLINE

/**
 * Los últimos [aiCount] identificadores de una partida de [playerCount] jugadores.
 * El humano principal es siempre [PlayerId] 0.
 */
internal fun aiPlayersForCount(playerCount: Int, aiCount: Int): Set<PlayerId> =
    ((playerCount - aiCount) until playerCount).map { PlayerId(it) }.toSet()

/** Configuración de partida asociada a un [GameMode]. */
internal fun GameMode.toGameConfig(): GameConfig = GameConfig(playerCount = playerCount)

/**
 * Setup de presentación para una partida de [playerCount] jugadores en la que la
 * IA controla a [aiPlayers] con la [difficulty] indicada. Si no hay jugadores IA,
 * la dificultad se descarta.
 */
internal fun buildGameSetup(
    playerCount: Int,
    aiPlayers: Set<PlayerId>,
    difficulty: AiDifficulty?,
): GameSetup =
    GameSetup(
        config = GameConfig(playerCount = playerCount),
        aiPlayers = aiPlayers,
        difficulty = if (aiPlayers.isNotEmpty()) difficulty else null,
    )

/**
 * Setup de partida para este modo con [aiCount] jugadores IA y la [difficulty]
 * elegida. La IA controla los últimos [aiCount] jugadores.
 */
internal fun GameMode.toGameSetup(
    aiCount: Int = this.aiCount,
    difficulty: AiDifficulty? = null,
): GameSetup =
    buildGameSetup(
        playerCount = playerCount,
        aiPlayers = aiPlayersForCount(playerCount, aiCount),
        difficulty = difficulty,
    )
