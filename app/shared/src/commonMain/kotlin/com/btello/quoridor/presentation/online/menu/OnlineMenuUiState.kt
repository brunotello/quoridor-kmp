package com.btello.quoridor.presentation.online.menu

/**
 * Estado de UI de la pantalla online principal (feature `online`).
 *
 * [playerName] es el nombre del jugador local (persistido en preferencias).
 * [nameConfirmed] indica si ya se definió un nombre y por lo tanto se muestra el
 * menú de acciones en lugar del paso de ingreso de nombre.
 */
internal data class OnlineMenuUiState(
    val playerName: String = "",
    val nameConfirmed: Boolean = false,
) {
    private val hasName: Boolean get() = playerName.isNotBlank()

    /** El nombre puede confirmarse cuando no está vacío. */
    val canConfirmName: Boolean get() = hasName
}
