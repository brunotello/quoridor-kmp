package com.btello.quoridor.presentation.settings

/**
 * Estado de UI de la pantalla de ajustes (feature `settings`).
 *
 * Contiene únicamente lo que gestiona el [SettingsViewModel]: el nombre del
 * jugador local y la visibilidad de los diálogos. El tema y el idioma se izan
 * hacia capas superiores (estado de la app) y llegan como parámetros.
 */
internal data class SettingsUiState(
    val playerName: String = "",
    val showNameDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
)
