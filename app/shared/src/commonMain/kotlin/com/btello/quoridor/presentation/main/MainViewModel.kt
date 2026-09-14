package com.btello.quoridor.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel de la pantalla "Nueva partida" (feature `menu`).
 *
 * Emite un [MainSideEffect] cuando el usuario elige un modo disponible para
 * comenzar la partida. Los modos deshabilitados se ignoran.
 */
internal class MainViewModel : ViewModel() {

    var uiState by mutableStateOf(MainUiState())
        private set

    private val _sideEffects = Channel<MainSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<MainSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.SelectMode -> {
                if (event.mode.enabled) {
                    _sideEffects.trySend(MainSideEffect.NavigateToGame(event.mode.toGameConfig()))
                }
            }
        }
    }
}
