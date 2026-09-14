package com.btello.quoridor.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.game.GameSetup
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.game_mode_pvai
import quoridor.app.shared.generated.resources.game_mode_pvai_description
import quoridor.app.shared.generated.resources.game_mode_pvp
import quoridor.app.shared.generated.resources.game_mode_pvp_description

/**
 * Modos de juego ofrecidos en la pantalla "Nueva partida".
 *
 * Cada modo declara su título, descripción, icono (Material Icons), si está
 * disponible, la cantidad de jugadores con la que arranca la partida y si el
 * rival es la IA. Agregar un modo sólo requiere una entrada aquí (más sus
 * strings).
 */
internal enum class GameMode(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val enabled: Boolean,
    val playerCount: Int,
    val aiOpponent: Boolean,
) {
    VERSUS_AI(
        titleRes = Res.string.game_mode_pvai,
        descriptionRes = Res.string.game_mode_pvai_description,
        icon = Icons.Outlined.SmartToy,
        enabled = true,
        playerCount = 2,
        aiOpponent = true,
    ),
    LOCAL_1V1(
        titleRes = Res.string.game_mode_pvp,
        descriptionRes = Res.string.game_mode_pvp_description,
        icon = Icons.Outlined.Person,
        enabled = true,
        playerCount = 2,
        aiOpponent = false,
    ),
}

/**
 * Al elegir un modo con [GameMode.aiOpponent] primero se elige la dificultad; el
 * resto de los modos arrancan la partida directamente.
 */
internal val GameMode.requiresDifficulty: Boolean
    get() = aiOpponent

/** Configuración de partida asociada a un [GameMode]. */
internal fun GameMode.toGameConfig(): GameConfig = GameConfig(playerCount = playerCount)

/**
 * Setup de partida para este modo. En los modos contra la IA, el segundo jugador
 * ([PlayerId] 1) queda controlado por la máquina con la [difficulty] indicada.
 */
internal fun GameMode.toGameSetup(difficulty: AiDifficulty? = null): GameSetup =
    GameSetup(
        config = toGameConfig(),
        aiPlayers = if (aiOpponent) setOf(PlayerId(1)) else emptySet(),
        difficulty = if (aiOpponent) difficulty else null,
    )
