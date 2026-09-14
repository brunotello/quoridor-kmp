package com.btello.quoridor.presentation.main

import com.btello.quoridor.domain.model.GameConfig

/**
 * Efectos de una sola vez emitidos por [MainViewModel] (feature `menu`).
 */
internal sealed interface MainSideEffect {
    data class NavigateToGame(val config: GameConfig) : MainSideEffect
}
