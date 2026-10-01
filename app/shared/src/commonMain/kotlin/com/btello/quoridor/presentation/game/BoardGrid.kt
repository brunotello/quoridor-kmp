package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation

/**
 * Modelo de grilla para el render del tablero.
 *
 * Un tablero de N x N celdas se representa como una grilla de (2N-1) x (2N-1) slots,
 * donde las posiciones pares corresponden a celdas de peón y las impares a las
 * ranuras (segmentos de muro) e intersecciones entre ellas.
 */
internal sealed interface BoardSlot {
    val row: Int
    val col: Int
}

internal data class CellSlot(
    override val row: Int,
    override val col: Int,
    val cell: Cell,
    val occupant: Player?,
    val isPawnTarget: Boolean,
    val isActivePawn: Boolean,
) : BoardSlot

internal data class WallSlot(
    override val row: Int,
    override val col: Int,
    val orientation: WallOrientation,
    val isCovered: Boolean,
    val isHighlighted: Boolean = false,
) : BoardSlot

internal data class IntersectionSlot(
    override val row: Int,
    override val col: Int,
    val isCovered: Boolean,
    val isHighlighted: Boolean = false,
) : BoardSlot

/**
 * [highlightedWalls] son los muros legales a resaltar (p. ej. mientras se arrastra
 * un muro); cada ranura o intersección que cubran se marca como resaltada.
 */
internal class BoardGrid(
    val state: GameState,
    private val legalTargets: Set<Cell>,
    private val highlightedWalls: Set<Wall> = emptySet(),
) {
    val boardSize: Int = state.board.size
    val gridSize: Int = boardSize * 2 - 1

    private val activePlayer: Player = state.players.first { it.id == state.turn.playerId }
    private val placedWalls: Set<Wall> = state.board.walls

    fun slotAt(row: Int, col: Int): BoardSlot {
        val rowIsCell = row % 2 == 0
        val colIsCell = col % 2 == 0
        return when {
            rowIsCell && colIsCell -> buildCellSlot(row, col)
            rowIsCell -> buildVerticalWallSlot(row, col)
            colIsCell -> buildHorizontalWallSlot(row, col)
            else -> IntersectionSlot(
                row = row,
                col = col,
                isCovered = horizontalCovers(row, col) || verticalCovers(row, col),
                isHighlighted = highlightedWalls.any { it.covers(row, col) },
            )
        }
    }

    private fun buildCellSlot(row: Int, col: Int): CellSlot {
        val cell = Cell(row / 2, col / 2)
        return CellSlot(
            row = row,
            col = col,
            cell = cell,
            occupant = state.players.firstOrNull { it.position == cell },
            isPawnTarget = cell in legalTargets,
            isActivePawn = cell == activePlayer.position,
        )
    }

    private fun buildVerticalWallSlot(row: Int, col: Int): WallSlot =
        WallSlot(
            row = row,
            col = col,
            orientation = WallOrientation.VERTICAL,
            isCovered = verticalCovers(row, col),
            isHighlighted = highlightedWalls.any { it.covers(row, col) },
        )

    private fun buildHorizontalWallSlot(row: Int, col: Int): WallSlot =
        WallSlot(
            row = row,
            col = col,
            orientation = WallOrientation.HORIZONTAL,
            isCovered = horizontalCovers(row, col),
            isHighlighted = highlightedWalls.any { it.covers(row, col) },
        )

    private fun horizontalCovers(row: Int, col: Int): Boolean =
        placedWalls.any { it.orientation == WallOrientation.HORIZONTAL && it.covers(row, col) }

    private fun verticalCovers(row: Int, col: Int): Boolean =
        placedWalls.any { it.orientation == WallOrientation.VERTICAL && it.covers(row, col) }
}

/** `true` si este muro ocupa el slot ([row], [col]) de la grilla de render. */
internal fun Wall.covers(row: Int, col: Int): Boolean = when (orientation) {
    WallOrientation.HORIZONTAL -> row == this.row * 2 + 1 && col in this.col * 2..this.col * 2 + 2
    WallOrientation.VERTICAL -> col == this.col * 2 + 1 && row in this.row * 2..this.row * 2 + 2
}
