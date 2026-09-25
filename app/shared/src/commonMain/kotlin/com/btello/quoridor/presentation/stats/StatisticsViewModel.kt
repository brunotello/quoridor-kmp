package com.btello.quoridor.presentation.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.btello.quoridor.domain.stats.GameStatistics
import com.btello.quoridor.domain.stats.StatisticsRepository

/**
 * ViewModel de la pantalla de estadísticas.
 *
 * Lee los registros persistidos a través del [StatisticsRepository] (dominio) y
 * los agrega en [GameStatistics]. Expone [refresh] para recalcular cuando la
 * pestaña vuelve a mostrarse tras jugar nuevas partidas.
 */
internal class StatisticsViewModel(
    private val repository: StatisticsRepository,
) : ViewModel() {

    var uiState by mutableStateOf(StatisticsUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        uiState = StatisticsUiState(GameStatistics.from(repository.records()))
    }
}
