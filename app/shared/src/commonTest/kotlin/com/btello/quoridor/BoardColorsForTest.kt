package com.btello.quoridor

import com.btello.quoridor.presentation.theme.DefaultBoardColors
import com.btello.quoridor.presentation.theme.LightBoardColors
import com.btello.quoridor.presentation.theme.boardColorsFor
import com.btello.quoridor.presentation.theme.green_500
import com.btello.quoridor.presentation.theme.green_700
import kotlin.test.Test
import kotlin.test.assertEquals

class BoardColorsForTest {

    @Test
    fun usesDarkPaletteInDarkMode() {
        assertEquals(DefaultBoardColors, boardColorsFor(darkTheme = true))
    }

    @Test
    fun usesLightPaletteInLightMode() {
        assertEquals(LightBoardColors, boardColorsFor(darkTheme = false))
    }

    @Test
    fun lightAndDarkPalettesDiffer() {
        assertEquals(false, LightBoardColors == DefaultBoardColors)
    }

    @Test
    fun lightModeUsesDarkerGreenThanDarkMode() {
        val greenIndex = 2
        assertEquals(green_700, LightBoardColors.players[greenIndex])
        assertEquals(green_500, DefaultBoardColors.players[greenIndex])
        assertEquals(false, LightBoardColors.players[greenIndex] == DefaultBoardColors.players[greenIndex])
    }
}
