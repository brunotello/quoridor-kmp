package com.btello.quoridor.presentation.stats

import com.btello.quoridor.domain.stats.GameStatistics

/**
 * Estado de UI de la pantalla de estadísticas (feature `stats`).
 */
internal data class StatisticsUiState(
    val statistics: GameStatistics = GameStatistics.EMPTY,
)
