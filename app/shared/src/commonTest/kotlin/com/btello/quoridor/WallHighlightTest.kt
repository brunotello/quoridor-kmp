package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation.HORIZONTAL
import com.btello.quoridor.domain.model.WallOrientation.VERTICAL
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.BoardGrid
import com.btello.quoridor.presentation.game.GameUiState
import com.btello.quoridor.presentation.game.IntersectionSlot
import com.btello.quoridor.presentation.game.WallSlot
import com.btello.quoridor.presentation.game.covers
import com.btello.quoridor.presentation.game.highlightedWalls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WallHighlightTest {

    private val state = QuoridorRules.startGame(GameConfig(playerCount = 2))

    @Test
    fun `horizontal wall covers its groove row across two cells and the intersection`() {
        val wall = Wall(0, 0, HORIZONTAL)
        assertTrue(wall.covers(1, 0))
        assertTrue(wall.covers(1, 1))
        assertTrue(wall.covers(1, 2))
        assertFalse(wall.covers(1, 3))
        assertFalse(wall.covers(0, 1))
    }

    @Test
    fun `vertical wall covers its groove column across two cells and the intersection`() {
        val wall = Wall(2, 3, VERTICAL)
        assertTrue(wall.covers(4, 7))
        assertTrue(wall.covers(5, 7))
        assertTrue(wall.covers(6, 7))
        assertFalse(wall.covers(7, 7))
        assertFalse(wall.covers(4, 6))
    }

    @Test
    fun `highlighted walls are empty when nothing is being dragged`() {
        val ui = GameUiState(gameState = state, legalWalls = setOf(Wall(0, 0, HORIZONTAL)))
        assertEquals(emptySet(), ui.highlightedWalls(null))
    }

    @Test
    fun `highlighted walls only include the dragged orientation`() {
        val h = Wall(0, 0, HORIZONTAL)
        val v = Wall(1, 1, VERTICAL)
        val ui = GameUiState(gameState = state, legalWalls = setOf(h, v))
        assertEquals(setOf(h), ui.highlightedWalls(HORIZONTAL))
        assertEquals(setOf(v), ui.highlightedWalls(VERTICAL))
    }

    @Test
    fun `board grid marks slots of highlighted walls`() {
        val grid = BoardGrid(state, emptySet(), setOf(Wall(0, 0, HORIZONTAL)))
        assertTrue((grid.slotAt(1, 0) as WallSlot).isHighlighted)
        assertTrue((grid.slotAt(1, 1) as IntersectionSlot).isHighlighted)
        assertFalse((grid.slotAt(1, 4) as WallSlot).isHighlighted)
        assertFalse((grid.slotAt(0, 1) as WallSlot).isHighlighted)
    }

    @Test
    fun `board grid highlights nothing by default`() {
        val grid = BoardGrid(state, emptySet())
        assertFalse((grid.slotAt(1, 0) as WallSlot).isHighlighted)
    }
}
