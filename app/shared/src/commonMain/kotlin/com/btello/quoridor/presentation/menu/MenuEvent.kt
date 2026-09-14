package com.btello.quoridor.presentation.menu

/**
 * Intenciones del usuario en la pantalla principal (feature `menu`).
 */
internal sealed interface MenuEvent {
    data class SelectPlayerCount(val count: Int) : MenuEvent
    data object StartGame : MenuEvent
}
