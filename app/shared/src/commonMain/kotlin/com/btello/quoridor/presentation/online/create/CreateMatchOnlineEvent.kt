package com.btello.quoridor.presentation.online.create

import com.btello.quoridor.domain.online.SeriesFormat

/**
 * Intenciones del usuario en la pantalla de creación de una sala online.
 */
internal sealed interface CreateMatchOnlineEvent {
    /** El usuario elige la cantidad de jugadores de la sala a crear (2 o 4). */
    data class PlayerCountChanged(val count: Int) : CreateMatchOnlineEvent

    /** El usuario cambia si la sala a crear es pública (visible en el lobby). */
    data class VisibilityChanged(val isPublic: Boolean) : CreateMatchOnlineEvent

    /** El usuario elige el formato de la serie competitiva (1 juego, mejor de 3 o 5). */
    data class FormatChanged(val format: SeriesFormat) : CreateMatchOnlineEvent

    /** El usuario elige el tiempo del temporizador en minutos, o `null` para desactivarlo. */
    data class TimeControlChanged(val minutes: Int?) : CreateMatchOnlineEvent

    /** Crear una sala nueva y esperar a que se unan los rivales. */
    data object CreateMatch : CreateMatchOnlineEvent

    /** Cancelar la espera / creación y volver al estado inicial. */
    data object Cancel : CreateMatchOnlineEvent
}
