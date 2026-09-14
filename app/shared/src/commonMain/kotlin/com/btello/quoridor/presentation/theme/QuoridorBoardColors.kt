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
    background = Color(0xFF0B1320),
    cell = Color(0xFF3A3A3A),
    cellTarget = Color(0xFF123557),
    cellActive = Color(0xFF17324D),
    wallPlaced = Color(0xFF5C5C5C),
    wallLegal = Color(0xFF4CC9F0),
    pawnLabel = Color(0xFFFFFFFF),
    wallReserve = Color(0xFF000000),
    players = listOf(
        Color(0xFF2DA8FF),
        Color(0xFFFF4D6D),
        Color(0xFF6CFF8F),
        Color(0xFFFFA94D),
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
