package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.Wall

/**
 * Intenciones del usuario en la pantalla de juego (feature `game`).
 */
internal sealed interface GameEvent {
    data object ActivePawnClick : GameEvent
    data class CellClick(val cell: Cell) : GameEvent

    /** Se soltó el dedo sobre el tablero para colocar [wall] tras arrastrarlo desde un botón. */
    data class WallDrop(val wall: Wall) : GameEvent
    data object LeaveMatch : GameEvent
    data object NewGame : GameEvent

    /** Online competitivo: avanzar al siguiente juego de la serie. */
    data object ContinueSeries : GameEvent
}
