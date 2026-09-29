package com.btello.quoridor.presentation.online.create

import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.online.OnlineError

/** Cantidades de jugadores admitidas al crear una sala online. */
internal const val MIN_PLAYERS = 2
internal const val MAX_PLAYERS = 4

/**
 * Controles de tiempo (en minutos) ofrecidos para el temporizador competitivo.
 * `null` desactiva el reloj (sin límite).
 */
internal val TIME_CONTROL_OPTIONS: List<Int?> = listOf(null, 5, 10)

/** Segundos por minuto, para convertir el control de tiempo elegido en minutos. */
internal const val SECONDS_PER_MINUTE = 60

/** Fase actual de la creación de una sala. */
internal enum class CreateMatchOnlinePhase {
    Idle,
    Creating,
    WaitingForOpponent,
}

/**
 * Estado de UI de la pantalla de creación de una sala online.
 *
 * [playerCount] es la cantidad de jugadores elegida (2 o 4); [isPublic] indica si
 * la sala se anuncia en el lobby público; [format] y [timeControlMinutes] son las
 * opciones competitivas (serie al mejor de N y temporizador estilo ajedrez, ambas
 * opcionales); [hostedCode] es el código a compartir mientras se espera;
 * [joinedCount] cuántos jugadores ya se unieron.
 */
internal data class CreateMatchOnlineUiState(
    val phase: CreateMatchOnlinePhase = CreateMatchOnlinePhase.Idle,
    val playerName: String = "",
    val playerCount: Int = MIN_PLAYERS,
    val isPublic: Boolean = true,
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val timeControlMinutes: Int? = null,
    val hostedCode: String? = null,
    val joinedCount: Int = 1,
    val error: OnlineError? = null,
) {
    /** Sólo se puede crear con nombre definido y sin otra operación en curso. */
    val canCreate: Boolean get() = playerName.isNotBlank() && phase == CreateMatchOnlinePhase.Idle
}
