package com.btello.quoridor.presentation.online.find

import com.btello.quoridor.domain.online.MatchId

/**
 * Intenciones del usuario en la pantalla de búsqueda de salas públicas.
 */
internal sealed interface FindMatchesOnlineEvent {
    /** El usuario refresca el listado de salas públicas. */
    data object Refresh : FindMatchesOnlineEvent

    /** Unirse a una sala pública elegida de la lista, sin ingresar código. */
    data class JoinPublicMatch(val id: MatchId) : FindMatchesOnlineEvent
}
