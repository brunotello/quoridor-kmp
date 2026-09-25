package com.btello.quoridor.presentation.online

import com.btello.quoridor.presentation.game.GameSetup

/**
 * Efectos de una sola vez emitidos por [OnlineLobbyViewModel] (feature `online`).
 */
internal sealed interface OnlineLobbySideEffect {
    /** La sala está lista (ambos jugadores presentes): arranca la partida online. */
    data class StartGame(val setup: GameSetup) : OnlineLobbySideEffect
}
