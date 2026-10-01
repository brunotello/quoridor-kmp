package com.btello.quoridor.presentation.main

/**
 * Efectos de una sola vez emitidos por [MainViewModel] (feature `menu`).
 */
internal sealed interface MainSideEffect {
    /** Abre la pantalla de configuración de una partida local para el [mode] elegido. */
    data class NavigateToLocalSetup(val mode: GameMode) : MainSideEffect

    /** Abre el lobby online (crear/unirse a una sala). */
    data object NavigateToOnlineLobby : MainSideEffect
}
