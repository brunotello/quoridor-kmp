package com.btello.quoridor.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores extendidos del tablero que no tienen un rol directo en el
 * [androidx.compose.material3.ColorScheme] de Material 3.
 *
 * Se definen a nivel de tema (no en los componentes) y se exponen mediante
 * [LocalBoardColors], respetando la regla de no hardcodear colores en la UI.
 */
internal data class QuoridorBoardColors(
    val background: Color,
    val cell: Color,
    val cellTarget: Color,
    val cellActive: Color,
    val wallPlaced: Color,
    val wallLegal: Color,
    val pawnLabel: Color,
    val wallReserve: Color,
    val players: List<Color>,
)

internal val DefaultBoardColors: QuoridorBoardColors = QuoridorBoardColors(
    background = blue_900,
    cell = gray_700,
    cellTarget = blue_700,
    cellActive = blue_800,
    wallPlaced = gray_500,
    wallLegal = cyan_0,
    pawnLabel = gray_0,
    wallReserve = gray_900,
    players = listOf(
        blue_0,
        pink_500,
        green_500,
        orange_500,
    ),
)

internal val LocalBoardColors = staticCompositionLocalOf { DefaultBoardColors }

/**
 * Índice de color de peón para un identificador de jugador dado.
 *
 * Se extrae como función pura para poder testearla sin infraestructura de Compose.
 */
internal fun playerColorIndex(id: Int, paletteSize: Int): Int {
    require(paletteSize > 0) { "La paleta de colores no puede estar vacía" }
    val normalized = id % paletteSize
    return if (normalized < 0) normalized + paletteSize else normalized
}
