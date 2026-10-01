package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation

/**
 * Geometría (sin dependencias de Compose) para traducir la posición de un dedo
 * arrastrando sobre el tablero a la ranura de muro correspondiente.
 *
 * Un tablero de `N x N` celdas cuadradas de lado `cellSize` separadas por una
 * separación `gap` ocupa `sizePx` px por lado. Cada celda `i` empieza en
 * `i * step` (con `step = cellSize + gap`). Los muros se colocan en las ranuras
 * entre celdas: un muro `Wall(row, col)` cubre dos celdas contiguas y su índice
 * va de `0` a `N - 2`.
 */
internal object WallPlacement {

    /**
     * Métricas del área de contenido del tablero medidas en coordenadas de la
     * raíz, necesarias para mapear la posición global del dedo a un muro.
     *
     * [left]/[top] son el origen del contenido (ya descontado el padding), [sizePx]
     * el lado del tablero, [gapPx] la separación entre celdas y [boardSize] la
     * cantidad de celdas por lado.
     */
    data class BoardMetrics(
        val left: Float,
        val top: Float,
        val sizePx: Float,
        val gapPx: Float,
        val boardSize: Int,
    )

    /**
     * Muro cuya ranura queda más cercana a la posición global ([rootX], [rootY])
     * del dedo para la [orientation] dada, o `null` si el punto cae fuera del
     * tablero descrito por [metrics].
     */
    fun wallAt(
        metrics: BoardMetrics,
        rootX: Float,
        rootY: Float,
        orientation: WallOrientation,
    ): Wall? {
        val px = rootX - metrics.left
        val py = rootY - metrics.top
        if (px < 0f || py < 0f || px > metrics.sizePx || py > metrics.sizePx) return null

        val n = metrics.boardSize
        if (n < 2) return null
        val cellSize = cellSizePx(metrics)
        val step = cellSize + metrics.gapPx
        val grooveOffset = cellSize + metrics.gapPx / 2f
        val spanOffset = (step + cellSize) / 2f
        val maxIndex = n - 2

        return when (orientation) {
            WallOrientation.HORIZONTAL -> {
                val row = nearestIndex(py - grooveOffset, step, maxIndex)
                val col = nearestIndex(px - spanOffset, step, maxIndex)
                Wall(row, col, orientation)
            }
            WallOrientation.VERTICAL -> {
                val col = nearestIndex(px - grooveOffset, step, maxIndex)
                val row = nearestIndex(py - spanOffset, step, maxIndex)
                Wall(row, col, orientation)
            }
        }
    }

    /** Largo en px de un muro (dos celdas más la separación entre ellas) según [metrics]. */
    fun wallLengthPx(metrics: BoardMetrics): Float = cellSizePx(metrics) * 2 + metrics.gapPx

    private fun cellSizePx(metrics: BoardMetrics): Float =
        (metrics.sizePx - metrics.gapPx * (metrics.boardSize - 1)) / metrics.boardSize

    private fun nearestIndex(value: Float, step: Float, maxIndex: Int): Int {
        val index = (value / step + 0.5f).toInt()
        return index.coerceIn(0, maxIndex)
    }
}
