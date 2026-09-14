package com.btello.quoridor.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val QuoridorDarkColorScheme = darkColorScheme(
    primary = blue_100,
    secondary = pink_500,
)

private val QuoridorLightColorScheme = lightColorScheme(
    primary = blue_100,
    secondary = pink_500,
)

/**
 * Tema raíz de la app: Material 3 (claro u oscuro) + tipografía y colores propios.
 */
@Composable
internal fun QuoridorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBoardColors provides DefaultBoardColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) QuoridorDarkColorScheme else QuoridorLightColorScheme,
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
