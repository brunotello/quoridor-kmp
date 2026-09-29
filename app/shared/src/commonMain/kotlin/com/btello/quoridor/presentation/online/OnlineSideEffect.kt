package com.btello.quoridor.presentation.online

import com.btello.quoridor.presentation.game.GameSetup

/**
 * Efectos de una sola vez comunes a las pantallas online (crear, unirse, buscar).
 */
internal sealed interface OnlineSideEffect {
    /** La sala está lista: arranca la partida online con el [setup] indicado. */
    data class StartGame(val setup: GameSetup) : OnlineSideEffect
}
