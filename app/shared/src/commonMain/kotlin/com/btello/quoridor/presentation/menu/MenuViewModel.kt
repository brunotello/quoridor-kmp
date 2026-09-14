package com.btello.quoridor.presentation.menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.btello.quoridor.domain.model.GameConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel de la pantalla principal "iniciar juego" (feature `menu`).
 *
 * Gestiona la selección de configuración y emite un [MenuSideEffect] cuando
 * el usuario decide comenzar la partida.
 */
internal class MenuViewModel : ViewModel() {

    var uiState by mutableStateOf(MenuUiState())
        private set

    private val _sideEffects = Channel<MenuSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<MenuSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: MenuEvent) {
        when (event) {
            is MenuEvent.SelectPlayerCount -> uiState = uiState.copy(playerCount = event.count)
            MenuEvent.StartGame ->
                _sideEffects.trySend(MenuSideEffect.NavigateToGame(GameConfig(playerCount = uiState.playerCount)))
        }
    }
}
