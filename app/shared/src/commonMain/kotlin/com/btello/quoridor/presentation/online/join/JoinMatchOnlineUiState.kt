package com.btello.quoridor.presentation.online.join

import com.btello.quoridor.presentation.online.OnlineError

/** Fase actual de la unión por código. */
internal enum class JoinMatchOnlinePhase {
    Idle,
    Joining,
}

/**
 * Estado de UI de la pantalla de unión a una sala online por código.
 *
 * [joinCode] es el texto ingresado para unirse a una sala existente.
 */
internal data class JoinMatchOnlineUiState(
    val phase: JoinMatchOnlinePhase = JoinMatchOnlinePhase.Idle,
    val playerName: String = "",
    val joinCode: String = "",
    val error: OnlineError? = null,
) {
    private val canJoin: Boolean get() = playerName.isNotBlank() && phase == JoinMatchOnlinePhase.Idle

    /** El botón de unirse por código exige además un código no vacío. */
    val canJoinByCode: Boolean get() = canJoin && joinCode.isNotBlank()
}
