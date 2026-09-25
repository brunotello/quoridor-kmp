package com.btello.quoridor.presentation.online

/**
 * Intenciones del usuario en el lobby online (feature `online`).
 */
internal sealed interface OnlineLobbyEvent {
    /** El usuario edita su nombre de jugador. */
    data class NameChanged(val name: String) : OnlineLobbyEvent

    /** El usuario confirma su nombre y avanza al menú de acciones. */
    data object ConfirmName : OnlineLobbyEvent

    /** El usuario elige la acción "crear partida". */
    data object ChooseCreate : OnlineLobbyEvent

    /** El usuario elige la acción "unirse a una partida". */
    data object ChooseJoin : OnlineLobbyEvent

    /** El usuario elige la cantidad de jugadores de la sala a crear (2 o 4). */
    data class PlayerCountChanged(val count: Int) : OnlineLobbyEvent

    /** Crear una sala nueva y esperar a que se unan los rivales. */
    data object CreateMatch : OnlineLobbyEvent

    /** El usuario edita el código de sala al que quiere unirse. */
    data class JoinCodeChanged(val code: String) : OnlineLobbyEvent

    /** Unirse a la sala con el código ingresado. */
    data object JoinMatch : OnlineLobbyEvent

    /** Volver al paso anterior del flujo (p. ej. de crear/unirse al menú). */
    data object NavigateBack : OnlineLobbyEvent

    /** Cancelar la espera / creación y volver al estado inicial. */
    data object Cancel : OnlineLobbyEvent
}
