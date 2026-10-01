package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.PlayerId
import kotlinx.serialization.Serializable

/** Milisegundos por segundo, para convertir el tiempo por turno configurado. */
private const val MILLIS_PER_SECOND = 1000L

/**
 * Cantidad de rondas de una partida competitiva online: la gana quien primero
 * alcance [gamesToWin] victorias. El modo por defecto ([SINGLE]) es una única
 * ronda (comportamiento clásico); [FIRST_TO_3] y [FIRST_TO_5] requieren 3 o 5.
 */
@Serializable
enum class SeriesFormat {
    /** Una sola ronda decide la partida. */
    SINGLE,

    /** Gana quien primero gane 3 rondas. */
    FIRST_TO_3,

    /** Gana quien primero gane 5 rondas. */
    FIRST_TO_5;

    /** Cantidad de rondas que un jugador debe ganar para llevarse la partida. */
    val gamesToWin: Int
        get() = when (this) {
            SINGLE -> 1
            FIRST_TO_3 -> 3
            FIRST_TO_5 -> 5
        }
}

/**
 * Configuración competitiva elegida al crear una sala online. Ambas opciones son
 * opcionales: [format] por defecto disputa una única ronda y [turnTimeSeconds]
 * nulo desactiva el temporizador. Si tiene valor, cada turno dura como máximo esos
 * segundos: al agotarse, el jugador no pierde la ronda sino que pierde el turno
 * (no mueve ni coloca muro) y este pasa al siguiente rival.
 */
@Serializable
data class CompetitiveConfig(
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val turnTimeSeconds: Int? = null,
) {
    init {
        require(turnTimeSeconds == null || turnTimeSeconds > 0) {
            "Turn time must be positive when set"
        }
    }

    /** `true` cuando la partida usa temporizador por turno. */
    val hasTimer: Boolean get() = turnTimeSeconds != null

    /** `true` cuando la partida activa alguna opción competitiva (rondas o temporizador). */
    val isCompetitive: Boolean get() = format != SeriesFormat.SINGLE || hasTimer

    /**
     * Tiempo restante del turno en curso tras [elapsedMillis] transcurridos, en
     * milisegundos (nunca negativo), o `null` si la partida no tiene temporizador.
     */
    fun remainingTurnMillis(elapsedMillis: Long): Long? =
        turnTimeSeconds?.let { (it * MILLIS_PER_SECOND - elapsedMillis).coerceAtLeast(0L) }

    /** `true` cuando hay temporizador y el turno en curso ya agotó su tiempo. */
    fun isTurnExpired(elapsedMillis: Long): Boolean = remainingTurnMillis(elapsedMillis) == 0L

    companion object {
        /** Duraciones de turno (en segundos) ofrecidas para el temporizador. */
        val TURN_TIME_OPTIONS: List<Int> = listOf(30, 45, 60)
    }
}

/**
 * Estado en vivo de una partida competitiva por rondas, sincronizado junto con la
 * partida.
 *
 * [wins] cuenta las victorias por jugador (indexado por [PlayerId.value]) y
 * [gameIndex] es el número de ronda en curso (base 0).
 */
@Serializable
data class CompetitiveState(
    val config: CompetitiveConfig = CompetitiveConfig(),
    val wins: List<Int> = emptyList(),
    val gameIndex: Int = 0,
) {
    /** `true` cuando algún jugador ya alcanzó las victorias necesarias. */
    val isSeriesOver: Boolean get() = wins.any { it >= config.format.gamesToWin }

    /** Ganador de la partida ([PlayerId]) o `null` si todavía no está definida. */
    val seriesWinner: PlayerId?
        get() = wins.indexOfFirst { it >= config.format.gamesToWin }
            .takeIf { it >= 0 }
            ?.let { PlayerId(it) }

    companion object {
        /**
         * Estado inicial para [playerCount] jugadores con la configuración
         * [config]: sin victorias y en la primera ronda.
         */
        fun initial(playerCount: Int, config: CompetitiveConfig): CompetitiveState =
            CompetitiveState(
                config = config,
                wins = List(playerCount) { 0 },
                gameIndex = 0,
            )
    }
}
