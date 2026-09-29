package com.btello.quoridor.presentation.settings

/**
 * Intenciones del usuario en la pantalla de ajustes (feature `settings`).
 */
internal sealed interface SettingsEvent {
    data object ShowNameDialog : SettingsEvent
    data object DismissNameDialog : SettingsEvent
    data class ConfirmName(val name: String) : SettingsEvent
    data object ShowLanguageDialog : SettingsEvent
    data object DismissLanguageDialog : SettingsEvent
}
