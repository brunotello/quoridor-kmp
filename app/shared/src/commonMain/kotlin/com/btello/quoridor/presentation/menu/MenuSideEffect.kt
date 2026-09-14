package com.btello.quoridor.presentation.menu

import com.btello.quoridor.domain.model.GameConfig

/**
 * Efectos de una sola vez emitidos por [MenuViewModel] (feature `menu`).
 */
internal sealed interface MenuSideEffect {
    data class NavigateToGame(val config: GameConfig) : MenuSideEffect
}
