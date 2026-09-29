package com.btello.quoridor.presentation.online.join

/**
 * Intenciones del usuario en la pantalla de unión por código.
 */
internal sealed interface JoinMatchOnlineEvent {
    /** El usuario edita el código de sala al que quiere unirse. */
    data class JoinCodeChanged(val code: String) : JoinMatchOnlineEvent

    /** Unirse a la sala con el código ingresado. */
    data object JoinMatch : JoinMatchOnlineEvent
}
