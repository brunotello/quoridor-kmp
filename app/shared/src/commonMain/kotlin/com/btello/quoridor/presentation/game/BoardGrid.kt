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
    val wall: Wall?,
    val isCovered: Boolean,
    val isLegal: Boolean,
) : BoardSlot

internal data class IntersectionSlot(
    override val row: Int,
    override val col: Int,
    val isCovered: Boolean,
    val isLegal: Boolean,
) : BoardSlot

internal class BoardGrid(
    val state: GameState,
    private val legalTargets: Set<Cell>,
    private val legalWalls: Set<Wall>,
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
                isLegal = intersectionIsLegal(row, col),
            )
        }
    }

    private fun intersectionIsLegal(row: Int, col: Int): Boolean {
        val wr = (row - 1) / 2
        val wc = (col - 1) / 2
        return Wall(wr, wc, WallOrientation.VERTICAL) in legalWalls ||
            Wall(wr, wc, WallOrientation.HORIZONTAL) in legalWalls
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

    private fun buildVerticalWallSlot(row: Int, col: Int): WallSlot {
        val cellRow = row / 2
        val wc = (col - 1) / 2
        val topWall = if (cellRow <= boardSize - 2) Wall(cellRow, wc, WallOrientation.VERTICAL) else null
        val bottomWall = if (cellRow - 1 in 0..boardSize - 2) Wall(cellRow - 1, wc, WallOrientation.VERTICAL) else null
        val legalWall = when {
            topWall != null && topWall in legalWalls -> topWall
            bottomWall != null && bottomWall in legalWalls -> bottomWall
            else -> null
        }
        return WallSlot(
            row = row,
            col = col,
            orientation = WallOrientation.VERTICAL,
            wall = legalWall ?: topWall,
            isCovered = verticalCovers(row, col),
            isLegal = legalWall != null,
        )
    }

    private fun buildHorizontalWallSlot(row: Int, col: Int): WallSlot {
        val wr = (row - 1) / 2
        val cellCol = col / 2
        val leftWall = if (cellCol <= boardSize - 2) Wall(wr, cellCol, WallOrientation.HORIZONTAL) else null
        val rightWall = if (cellCol - 1 in 0..boardSize - 2) Wall(wr, cellCol - 1, WallOrientation.HORIZONTAL) else null
        val legalWall = when {
            leftWall != null && leftWall in legalWalls -> leftWall
            rightWall != null && rightWall in legalWalls -> rightWall
            else -> null
        }
        return WallSlot(
            row = row,
            col = col,
            orientation = WallOrientation.HORIZONTAL,
            wall = legalWall ?: leftWall,
            isCovered = horizontalCovers(row, col),
            isLegal = legalWall != null,
        )
    }

    private fun horizontalCovers(row: Int, col: Int): Boolean =
        placedWalls.any {
            it.orientation == WallOrientation.HORIZONTAL &&
                row == it.row * 2 + 1 &&
                col in it.col * 2..it.col * 2 + 2
        }

    private fun verticalCovers(row: Int, col: Int): Boolean =
        placedWalls.any {
            it.orientation == WallOrientation.VERTICAL &&
                col == it.col * 2 + 1 &&
                row in it.row * 2..it.row * 2 + 2
        }
}
