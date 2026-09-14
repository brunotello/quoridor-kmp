package com.btello.quoridor.presentation.main

/**
 * Intenciones del usuario en la pantalla "Nueva partida" (feature `menu`).
 */
internal sealed interface MainEvent {
    data class SelectMode(val mode: GameMode) : MainEvent
}
