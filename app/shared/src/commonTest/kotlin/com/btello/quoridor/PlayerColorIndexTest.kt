package com.btello.quoridor

import com.btello.quoridor.presentation.theme.playerColorIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerColorIndexTest {

    @Test
    fun mapsEachPlayerToItsOwnSlotWithinPalette() {
        assertEquals(0, playerColorIndex(0, 4))
        assertEquals(1, playerColorIndex(1, 4))
        assertEquals(2, playerColorIndex(2, 4))
        assertEquals(3, playerColorIndex(3, 4))
    }

    @Test
    fun wrapsAroundWhenIdExceedsPaletteSize() {
        assertEquals(0, playerColorIndex(4, 4))
        assertEquals(1, playerColorIndex(5, 4))
        assertEquals(1, playerColorIndex(3, 2))
    }

    @Test
    fun normalizesNegativeIdsIntoValidRange() {
        assertEquals(3, playerColorIndex(-1, 4))
        assertEquals(0, playerColorIndex(-4, 4))
    }

    @Test
    fun rejectsEmptyPalette() {
        assertFailsWith<IllegalArgumentException> {
            playerColorIndex(0, 0)
        }
    }
}
