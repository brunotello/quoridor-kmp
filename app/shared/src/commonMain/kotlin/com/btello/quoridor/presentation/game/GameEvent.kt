package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.Wall

/**
 * Intenciones del usuario en la pantalla de juego (feature `game`).
 */
internal sealed interface GameEvent {
    data object ActivePawnClick : GameEvent
    data object WallReserveClick : GameEvent
    data class CellClick(val cell: Cell) : GameEvent
    data class WallClick(val wall: Wall) : GameEvent
    data object NewGame : GameEvent
}
