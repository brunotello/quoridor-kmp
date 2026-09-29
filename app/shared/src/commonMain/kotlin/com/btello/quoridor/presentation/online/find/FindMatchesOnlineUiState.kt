package com.btello.quoridor.presentation.online.find

import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineOpenMatch

/** Fase actual de la búsqueda de salas públicas. */
internal enum class FindMatchesOnlinePhase {
    Idle,
    Joining,
}

/**
 * Estado de UI de la pantalla de búsqueda de salas públicas.
 *
 * [openMatches] son las salas públicas disponibles para unirse sin código.
 */
internal data class FindMatchesOnlineUiState(
    val phase: FindMatchesOnlinePhase = FindMatchesOnlinePhase.Idle,
    val playerName: String = "",
    val openMatches: List<OnlineOpenMatch> = emptyList(),
    val error: OnlineError? = null,
) {
    /** Sólo se puede unir con nombre definido y sin otra operación en curso. */
    val canJoin: Boolean get() = playerName.isNotBlank() && phase == FindMatchesOnlinePhase.Idle
}
