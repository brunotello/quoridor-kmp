package com.btello.quoridor.presentation.navigationbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.nav_game
import quoridor.app.shared.generated.resources.nav_rules
import quoridor.app.shared.generated.resources.nav_settings

/**
 * Pestañas disponibles en el bottom navigation de la pantalla principal.
 *
 * Cada entrada declara su etiqueta e icono (Material Icons), de modo que agregar
 * una nueva pestaña sólo requiere añadir un valor aquí (más sus strings y su
 * contenido en [NavBar]).
 */
internal enum class Tabs(
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    GAME(
        labelRes = Res.string.nav_game,
        icon = Icons.Outlined.PlayArrow,
    ),
    RULES(
        labelRes = Res.string.nav_rules,
        icon = Icons.AutoMirrored.Outlined.List,
    ),
    SETTINGS(
        labelRes = Res.string.nav_settings,
        icon = Icons.Outlined.Settings,
    ),
}
