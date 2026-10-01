package com.btello.quoridor

import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.presentation.game.WallPlacement
import com.btello.quoridor.presentation.game.WallPlacement.BoardMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WallPlacementTest {

    // Tablero 9x9: 900 px, gap 10 px -> cellSize = (900 - 10*8)/9 = 91.11..., step = 101.11...
    private val metrics = BoardMetrics(
        left = 0f,
        top = 0f,
        sizePx = 900f,
        gapPx = 10f,
        boardSize = 9,
    )

    private val cellSize = (900f - 10f * 8) / 9f
    private val step = cellSize + 10f

    @Test
    fun `pointer outside the board returns null`() {
        assertNull(WallPlacement.wallAt(metrics, -1f, 100f, WallOrientation.HORIZONTAL))
        assertNull(WallPlacement.wallAt(metrics, 100f, 901f, WallOrientation.VERTICAL))
    }

    @Test
    fun `pointer over the first horizontal groove maps to wall row 0`() {
        // Centro de la primera ranura horizontal: y = cellSize + gap/2, x sobre las celdas 0-1.
        val x = cellSize + step * 0.5f
        val y = cellSize + 5f
        assertEquals(
            Wall(0, 0, WallOrientation.HORIZONTAL),
            WallPlacement.wallAt(metrics, x, y, WallOrientation.HORIZONTAL),
        )
    }

    @Test
    fun `pointer over an inner vertical groove maps to the right column`() {
        // Ranura vertical entre columnas 3 y 4: x = step*3 + cellSize + gap/2.
        val x = step * 3 + cellSize + 5f
        val y = step * 2 + cellSize
        assertEquals(
            Wall(2, 3, WallOrientation.VERTICAL),
            WallPlacement.wallAt(metrics, x, y, WallOrientation.VERTICAL),
        )
    }

    @Test
    fun `wall index is clamped to the last valid groove`() {
        // Cerca del borde inferior/derecho: debe acotarse a boardSize - 2 = 7.
        val wall = WallPlacement.wallAt(metrics, 899f, 899f, WallOrientation.HORIZONTAL)
        assertEquals(Wall(7, 7, WallOrientation.HORIZONTAL), wall)
    }

    @Test
    fun `wall length spans two cells plus the gap between them`() {
        assertEquals(cellSize * 2 + 10f, WallPlacement.wallLengthPx(metrics))
    }

    @Test
    fun `wall length scales with the board size in px`() {
        val small = metrics.copy(sizePx = 450f, gapPx = 5f)
        val smallCell = (450f - 5f * 8) / 9f
        assertEquals(smallCell * 2 + 5f, WallPlacement.wallLengthPx(small))
    }

    @Test
    fun `metrics offset is subtracted from the root position`() {
        val shifted = metrics.copy(left = 100f, top = 50f)
        val x = 100f + cellSize + step * 0.5f
        val y = 50f + cellSize + 5f
        assertEquals(
            Wall(0, 0, WallOrientation.HORIZONTAL),
            WallPlacement.wallAt(shifted, x, y, WallOrientation.HORIZONTAL),
        )
    }
}
