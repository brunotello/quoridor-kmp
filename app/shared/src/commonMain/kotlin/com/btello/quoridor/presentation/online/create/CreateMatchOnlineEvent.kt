package com.btello.quoridor.presentation.online.create

/**
 * Intenciones del usuario en la pantalla de creación de una sala online.
 */
internal sealed interface CreateMatchOnlineEvent {
    /** El usuario elige la cantidad de jugadores de la sala a crear (2 o 4). */
    data class PlayerCountChanged(val count: Int) : CreateMatchOnlineEvent

    /** El usuario cambia si la sala a crear es pública (visible en el lobby). */
    data class VisibilityChanged(val isPublic: Boolean) : CreateMatchOnlineEvent

    /** Crear una sala nueva y esperar a que se unan los rivales. */
    data object CreateMatch : CreateMatchOnlineEvent

    /** Cancelar la espera / creación y volver al estado inicial. */
    data object Cancel : CreateMatchOnlineEvent
}
