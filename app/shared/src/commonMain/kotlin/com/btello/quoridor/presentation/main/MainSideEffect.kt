package com.btello.quoridor.presentation.main

import com.btello.quoridor.presentation.game.GameSetup

/**
 * Efectos de una sola vez emitidos por [MainViewModel] (feature `menu`).
 */
internal sealed interface MainSideEffect {
    /** Arranca la partida con el setup indicado. */
    data class NavigateToGame(val setup: GameSetup) : MainSideEffect

    /** Abre el submenú de selección de dificultad para [aiCount] jugadores IA del [mode]. */
    data class NavigateToDifficulty(val mode: GameMode, val aiCount: Int) : MainSideEffect

    /** Abre el submenú de configuración (cantidad de IA) para una partida de 4 jugadores. */
    data class NavigateToPlayerSetup(val mode: GameMode) : MainSideEffect

    /** Abre el lobby online (crear/unirse a una sala). */
    data object NavigateToOnlineLobby : MainSideEffect
}
