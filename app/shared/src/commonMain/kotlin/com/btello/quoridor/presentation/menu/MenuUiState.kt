package com.btello.quoridor.presentation.menu

/**
 * Estado de UI de la pantalla principal "iniciar juego" (feature `menu`).
 */
internal data class MenuUiState(
    val playerCount: Int = 2,
    val playerCountOptions: List<Int> = listOf(2),
)
