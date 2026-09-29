package com.btello.quoridor.presentation.online.menu

/**
 * Intenciones del usuario en la pantalla online principal (feature `online`).
 */
internal sealed interface OnlineMenuEvent {
    /** El usuario edita su nombre de jugador. */
    data class NameChanged(val name: String) : OnlineMenuEvent

    /** El usuario confirma su nombre y avanza al menú de acciones. */
    data object ConfirmName : OnlineMenuEvent
}
