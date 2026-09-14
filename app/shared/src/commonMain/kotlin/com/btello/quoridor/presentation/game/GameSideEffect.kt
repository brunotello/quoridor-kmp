package com.btello.quoridor.presentation.game

/**
 * Efectos de una sola vez emitidos por [GameViewModel] (feature `game`).
 */
internal sealed interface GameSideEffect {
    data object NavigateToMenu : GameSideEffect
}
