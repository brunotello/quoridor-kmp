package com.btello.quoridor.presentation.main

import com.btello.quoridor.presentation.game.GameSetup

/**
 * Efectos de una sola vez emitidos por [MainViewModel] (feature `menu`).
 */
internal sealed interface MainSideEffect {
    /** Arranca la partida con el setup indicado. */
    data class NavigateToGame(val setup: GameSetup) : MainSideEffect

    /** Abre el submenú de selección de dificultad para un modo contra la IA. */
    data class NavigateToDifficulty(val mode: GameMode) : MainSideEffect
}
