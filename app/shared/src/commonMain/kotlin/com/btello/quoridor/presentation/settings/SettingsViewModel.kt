package com.btello.quoridor.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.btello.quoridor.data.player.PlayerNameRepository

/**
 * ViewModel de la pantalla de ajustes (feature `settings`).
 *
 * Gestiona el nombre del jugador local a través del [PlayerNameRepository]
 * (abstracción, respetando la regla de dependencias) y la visibilidad de los
 * diálogos. La lógica de negocio (recortar el nombre, persistirlo) vive aquí y
 * no en el `@Composable`.
 */
internal class SettingsViewModel(
    private val playerNameRepository: PlayerNameRepository,
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState(playerName = playerNameRepository.name()))
        private set

    fun onEvent(event: SettingsEvent) {
        uiState = when (event) {
            SettingsEvent.ShowNameDialog -> uiState.copy(showNameDialog = true)
            SettingsEvent.DismissNameDialog -> uiState.copy(showNameDialog = false)
            is SettingsEvent.ConfirmName -> {
                val trimmed = event.name.trim()
                playerNameRepository.setName(trimmed)
                uiState.copy(playerName = trimmed, showNameDialog = false)
            }

            SettingsEvent.ShowLanguageDialog -> uiState.copy(showLanguageDialog = true)
            SettingsEvent.DismissLanguageDialog -> uiState.copy(showLanguageDialog = false)
        }
    }
}
