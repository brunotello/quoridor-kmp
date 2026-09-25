package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.PlayerSlot

/**
 * Datos de una partida online activa para la capa de presentación: a qué sala
 * ([matchId]) está conectado este dispositivo y qué [slot] (asiento) controla.
 *
 * El [localPlayerId] deriva del asiento: el jugador controla el `PlayerId` cuyo
 * índice coincide con [PlayerSlot.index].
 */
internal data class OnlineSession(
    val matchId: MatchId,
    val slot: PlayerSlot,
) {
    val localPlayerId: PlayerId
        get() = slot.playerId
}
