package com.btello.quoridor.presentation.online

import com.btello.quoridor.domain.online.MatchId

/**
 * Resumen de una sala pública mostrada en el lobby para unirse sin código.
 */
internal data class OnlineOpenMatch(
    val id: MatchId,
    val hostName: String,
    val joinedCount: Int,
    val playerCount: Int,
)

/**
 * Estado de UI del lobby online (feature `online`).
 *
 * [step] indica en qué pantalla del flujo está el usuario (ingresar nombre,
 * elegir acción, crear, unirse por código o explorar salas públicas).
 * [playerName] es el nombre del jugador local (persistido en preferencias);
 * [playerCount] es la cantidad de jugadores elegida al crear una sala (2 o 4).
 * [isPublic] indica si la sala a crear se anuncia en el lobby público.
 * [hostedCode] es el código a compartir cuando este dispositivo crea la sala;
 * [joinCode] es el texto que se ingresa para unirse a una sala existente.
 * [joinedCount] indica cuántos jugadores ya se unieron mientras se espera.
 * [openMatches] son las salas públicas disponibles para unirse sin código.
 */
internal data class OnlineLobbyUiState(
    val step: OnlineLobbyStep = OnlineLobbyStep.Name,
    val phase: OnlineLobbyPhase = OnlineLobbyPhase.Idle,
    val playerName: String = "",
    val playerCount: Int = MIN_PLAYERS,
    val isPublic: Boolean = true,
    val joinCode: String = "",
    val hostedCode: String? = null,
    val joinedCount: Int = 1,
    val openMatches: List<OnlineOpenMatch> = emptyList(),
    val error: OnlineLobbyError? = null,
) {
    private val hasName: Boolean get() = playerName.isNotBlank()

    /** El nombre puede confirmarse cuando no está vacío y no hay operación en curso. */
    val canConfirmName: Boolean get() = hasName && phase == OnlineLobbyPhase.Idle

    /** Sólo se puede unirse con nombre definido y sin otra operación en curso. */
    val canJoin: Boolean get() = hasName && phase == OnlineLobbyPhase.Idle

    /** El botón de unirse por código además exige un código no vacío. */
    val canJoinByCode: Boolean get() = canJoin && joinCode.isNotBlank()

    /** Sólo se puede crear una sala con nombre definido y sin otra operación en curso. */
    val canCreate: Boolean get() = hasName && phase == OnlineLobbyPhase.Idle
}

/** Cantidades de jugadores admitidas al crear una sala online. */
internal const val MIN_PLAYERS = 2
internal const val MAX_PLAYERS = 4

/** Pantalla actual del flujo del lobby online. */
internal enum class OnlineLobbyStep {
    /** Ingreso del nombre del jugador (paso previo si aún no lo definió). */
    Name,

    /** Elección entre crear una partida o unirse a una existente. */
    Menu,

    /** Configuración y creación de una sala nueva (incluye la espera de rivales). */
    Create,

    /** Ingreso del código para unirse a una sala existente. */
    Join,

    /** Exploración de las salas públicas disponibles para unirse sin código. */
    Browse,
}

/** Fase actual del lobby. */
internal enum class OnlineLobbyPhase {
    Idle,
    Creating,
    WaitingForOpponent,
    Joining,
}

/** Errores mostrables del lobby, resueltos a texto en la capa Compose. */
internal enum class OnlineLobbyError {
    NotFound,
    NotJoinable,
    IncompatibleVersion,
    Connection,
    Unsupported,
}
