package com.btello.quoridor.presentation.online.menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.btello.quoridor.data.player.PlayerNameProvider
import com.btello.quoridor.data.player.PlayerNameRepository

/**
 * ViewModel de la pantalla online principal (feature `online`).
 *
 * Se encarga únicamente de resolver el nombre del jugador local: lo carga desde
 * [PlayerNameRepository], permite editarlo y lo persiste al confirmarlo. Una vez
 * definido el nombre, la pantalla muestra las tres acciones (crear, unirse,
 * buscar), cuya navegación gestiona la capa de UI.
 */
internal class OnlineMenuViewModel(
    private val playerNameRepository: PlayerNameRepository = PlayerNameProvider.repository,
) : ViewModel() {

    var uiState by mutableStateOf(
        playerNameRepository.name().let { name ->
            OnlineMenuUiState(playerName = name, nameConfirmed = name.isNotBlank())
        },
    )
        private set

    fun onEvent(event: OnlineMenuEvent) {
        when (event) {
            is OnlineMenuEvent.NameChanged -> uiState = uiState.copy(playerName = event.name)
            OnlineMenuEvent.ConfirmName -> confirmName()
        }
    }

    /** Persiste el nombre ingresado (recortado) y avanza al menú de acciones. */
    private fun confirmName() {
        if (!uiState.canConfirmName) return
        val name = uiState.playerName.trim()
        playerNameRepository.setName(name)
        uiState = uiState.copy(playerName = name, nameConfirmed = true)
    }
}
