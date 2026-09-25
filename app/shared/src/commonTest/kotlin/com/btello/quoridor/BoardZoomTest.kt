package com.btello.quoridor

import com.btello.quoridor.presentation.game.BoardZoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardZoomTest {

    @Test
    fun `nextScale multiplies by zoom factor within range`() {
        assertEquals(2f, BoardZoom.nextScale(1f, 2f))
        assertEquals(1.5f, BoardZoom.nextScale(3f, 0.5f))
    }

    @Test
    fun `nextScale never goes below min scale`() {
        assertEquals(BoardZoom.MIN_SCALE, BoardZoom.nextScale(1f, 0.1f))
        assertEquals(BoardZoom.MIN_SCALE, BoardZoom.nextScale(2f, 0f))
    }

    @Test
    fun `nextScale never exceeds max scale`() {
        assertEquals(BoardZoom.MAX_SCALE, BoardZoom.nextScale(2f, 5f))
        assertEquals(BoardZoom.MAX_SCALE, BoardZoom.nextScale(BoardZoom.MAX_SCALE, 1.2f))
    }

    @Test
    fun `maxTranslation is zero when not zoomed`() {
        assertEquals(0f, BoardZoom.maxTranslation(BoardZoom.MIN_SCALE, 900f))
    }

    @Test
    fun `maxTranslation grows with scale`() {
        assertEquals(450f, BoardZoom.maxTranslation(2f, 900f))
        assertEquals(900f, BoardZoom.maxTranslation(3f, 900f))
    }

    @Test
    fun `maxTranslation is never negative`() {
        assertTrue(BoardZoom.maxTranslation(0.5f, 900f) >= 0f)
    }

    @Test
    fun `clampTranslation pins offset to zero when not zoomed`() {
        assertEquals(0f, BoardZoom.clampTranslation(500f, BoardZoom.MIN_SCALE, 900f))
        assertEquals(0f, BoardZoom.clampTranslation(-500f, BoardZoom.MIN_SCALE, 900f))
    }

    @Test
    fun `isZoomed is false at base scale and true when magnified`() {
        assertTrue(!BoardZoom.isZoomed(BoardZoom.MIN_SCALE))
        assertTrue(BoardZoom.isZoomed(1.2f))
        assertTrue(BoardZoom.isZoomed(BoardZoom.MAX_SCALE))
    }

    @Test
    fun `clampTranslation keeps value inside allowed bounds`() {
        assertEquals(450f, BoardZoom.clampTranslation(1000f, 2f, 900f))
        assertEquals(-450f, BoardZoom.clampTranslation(-1000f, 2f, 900f))
        assertEquals(200f, BoardZoom.clampTranslation(200f, 2f, 900f))
    }
}
