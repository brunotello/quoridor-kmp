package com.btello.quoridor.presentation.main

/**
 * Estado de UI de la pantalla "Nueva partida" (feature `menu`).
 */
internal data class MainUiState(
    val modes: List<GameMode> = GameMode.entries,
)
