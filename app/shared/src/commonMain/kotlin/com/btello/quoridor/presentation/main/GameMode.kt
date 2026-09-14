package com.btello.quoridor.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.btello.quoridor.domain.model.GameConfig
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
 * disponible y la cantidad de jugadores con la que arranca la partida. Agregar
 * un modo sólo requiere una entrada aquí (más sus strings).
 */
internal enum class GameMode(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val enabled: Boolean,
    val playerCount: Int,
) {
    VERSUS_AI(
        titleRes = Res.string.game_mode_pvai,
        descriptionRes = Res.string.game_mode_pvai_description,
        icon = Icons.Outlined.SmartToy,
        enabled = false,
        playerCount = 2,
    ),
    LOCAL_1V1(
        titleRes = Res.string.game_mode_pvp,
        descriptionRes = Res.string.game_mode_pvp_description,
        icon = Icons.Outlined.Person,
        enabled = true,
        playerCount = 2,
    ),
}

/** Configuración de partida asociada a un [GameMode]. */
internal fun GameMode.toGameConfig(): GameConfig = GameConfig(playerCount = playerCount)
