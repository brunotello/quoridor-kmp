package com.btello.quoridor.presentation.navigationbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.nav_game
import quoridor.app.shared.generated.resources.nav_rules
import quoridor.app.shared.generated.resources.nav_settings
import quoridor.app.shared.generated.resources.nav_stats

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
        icon = Icons.Filled.PlayArrow,
    ),
    RULES(
        labelRes = Res.string.nav_rules,
        icon = Icons.AutoMirrored.Filled.List,
    ),
    STATS(
        labelRes = Res.string.nav_stats,
        icon = Icons.Filled.BarChart,
    ),
    SETTINGS(
        labelRes = Res.string.nav_settings,
        icon = Icons.Filled.Settings,
    ),
}
