package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.PlayerId
import kotlinx.serialization.Serializable

/**
 * Formato de una partida competitiva online. El modo por defecto ([SINGLE]) es un
 * único juego (comportamiento clásico); [FIRST_TO_3] y [FIRST_TO_5] disputan una
 * serie y la gana quien primero alcance [gamesToWin] victorias (3 o 5).
 */
@Serializable
enum class SeriesFormat {
    /** Un solo juego decide la partida. */
    SINGLE,

    /** Serie hasta que un jugador gane 3 juegos. */
    FIRST_TO_3,

    /** Serie hasta que un jugador gane 5 juegos. */
    FIRST_TO_5;

    /** Cantidad de juegos que un jugador debe ganar para llevarse la serie. */
    val gamesToWin: Int
        get() = when (this) {
            SINGLE -> 1
            FIRST_TO_3 -> 3
            FIRST_TO_5 -> 5
        }
}

/**
 * Configuración competitiva elegida al crear una sala online. Ambas opciones son
 * opcionales: [format] por defecto disputa un único juego y [timeControlSeconds]
 * nulo desactiva el temporizador estilo ajedrez; si tiene valor, cada jugador
 * arranca con ese tiempo total (en segundos) y su reloj corre en su turno.
 */
@Serializable
data class CompetitiveConfig(
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val timeControlSeconds: Int? = null,
) {
    init {
        require(timeControlSeconds == null || timeControlSeconds > 0) {
            "Time control must be positive when set"
        }
    }

    /** `true` cuando la partida usa temporizador. */
    val hasTimer: Boolean get() = timeControlSeconds != null

    /** `true` cuando la partida activa alguna opción competitiva (serie o reloj). */
    val isCompetitive: Boolean get() = format != SeriesFormat.SINGLE || hasTimer
}

/**
 * Estado en vivo de una serie competitiva, sincronizado junto con la partida.
 *
 * [wins] cuenta las victorias por jugador (indexado por [PlayerId.value]);
 * [gameIndex] es el número de juego en curso (base 0); [remainingMillis] es el
 * tiempo restante de cada jugador en milisegundos, o una lista vacía cuando la
 * partida no tiene temporizador.
 */
@Serializable
data class CompetitiveState(
    val config: CompetitiveConfig = CompetitiveConfig(),
    val wins: List<Int> = emptyList(),
    val gameIndex: Int = 0,
    val remainingMillis: List<Long> = emptyList(),
) {
    /** `true` cuando algún jugador ya alcanzó las victorias necesarias. */
    val isSeriesOver: Boolean get() = wins.any { it >= config.format.gamesToWin }

    /** Ganador de la serie ([PlayerId]) o `null` si todavía no está definida. */
    val seriesWinner: PlayerId?
        get() = wins.indexOfFirst { it >= config.format.gamesToWin }
            .takeIf { it >= 0 }
            ?.let { PlayerId(it) }

    companion object {
        /**
         * Estado inicial de una serie para [playerCount] jugadores con la
         * configuración [config]: sin victorias, primer juego y relojes en el
         * tiempo inicial (o vacíos si no hay temporizador).
         */
        fun initial(playerCount: Int, config: CompetitiveConfig): CompetitiveState =
            CompetitiveState(
                config = config,
                wins = List(playerCount) { 0 },
                gameIndex = 0,
                remainingMillis = config.timeControlSeconds
                    ?.let { seconds -> List(playerCount) { seconds * 1000L } }
                    ?: emptyList(),
            )
    }
}
