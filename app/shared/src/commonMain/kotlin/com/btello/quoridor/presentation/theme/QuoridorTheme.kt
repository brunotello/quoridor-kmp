package com.btello.quoridor.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Punto de acceso a los valores de tema propios de Quoridor.
 */
internal object QuoridorTheme {
    val boardColors: QuoridorBoardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBoardColors.current
}

/**
 * Tema raíz de la app: Material 3 (esquema oscuro) + tipografía y colores propios.
 */
@Composable
internal fun QuoridorTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBoardColors provides DefaultBoardColors) {
        MaterialTheme(
            colorScheme = darkColorScheme(),
            typography = QuoridorTypography,
            content = content,
        )
    }
}

/**
 * Color del peón/panel para un jugador, tomado siempre del tema.
 */
@Composable
@ReadOnlyComposable
internal fun playerColor(id: Int): Color {
    val palette = QuoridorTheme.boardColors.players
    return palette[playerColorIndex(id, palette.size)]
}
