package com.btello.quoridor.presentation.main

import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.domain.model.PlayerId
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
 * Cada modo declara su título, descripción, emoji, si está disponible y la
 * cantidad de jugadores con la que arranca por defecto. Los modos locales
 * ([VERSUS_AI], [LOCAL_1V1] y [FOUR_PLAYERS]) abren una pantalla de configuración
 * de la partida ([LocalMatchSetupScreen]); [ONLINE] abre el lobby. Agregar un modo
 * sólo requiere una entrada aquí (más sus strings).
 */
internal enum class GameMode(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val emojiRes: StringResource,
    val enabled: Boolean,
    val playerCount: Int,
) {
    VERSUS_AI(
        titleRes = Res.string.game_mode_pvai,
        descriptionRes = Res.string.game_mode_pvai_description,
        emojiRes = Res.string.game_mode_pvai_emoji,
        enabled = true,
        playerCount = 2,
    ),
    LOCAL_1V1(
        titleRes = Res.string.game_mode_pvp,
        descriptionRes = Res.string.game_mode_pvp_description,
        emojiRes = Res.string.game_mode_pvp_emoji,
        enabled = true,
        playerCount = 2,
    ),
    FOUR_PLAYERS(
        titleRes = Res.string.game_mode_4p,
        descriptionRes = Res.string.game_mode_4p_description,
        emojiRes = Res.string.game_mode_4p_emoji,
        enabled = true,
        playerCount = 4,
    ),
    ONLINE(
        titleRes = Res.string.game_mode_online,
        descriptionRes = Res.string.game_mode_online_description,
        emojiRes = Res.string.game_mode_online_emoji,
        enabled = OnlinePlatform.isSupported,
        playerCount = 2,
    ),
}

/** El modo online abre primero el lobby (crear/unirse a una sala) en vez de la configuración local. */
internal val GameMode.requiresLobby: Boolean
    get() = this == ONLINE

/**
 * Los últimos [aiCount] identificadores de una partida de [playerCount] jugadores.
 * El humano principal es siempre [PlayerId] 0.
 */
internal fun aiPlayersForCount(playerCount: Int, aiCount: Int): Set<PlayerId> =
    ((playerCount - aiCount) until playerCount).map { PlayerId(it) }.toSet()
