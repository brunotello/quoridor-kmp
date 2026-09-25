package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.PlayerId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Máxima cantidad de jugadores admitida por una partida online. */
const val MAX_ONLINE_PLAYERS: Int = 4

/**
 * Identificador (código de sala) de una partida online. Es corto y legible para
 * poder compartirlo entre los dos jugadores.
 */
@Serializable
@JvmInline
value class MatchId(val value: String) {
    init {
        require(value.isNotBlank()) { "Match id must not be blank" }
    }

    override fun toString(): String = value
}

/** Estado del ciclo de vida de una partida online. */
@Serializable
enum class MatchStatus {
    /** Sala creada, esperando a que se una el rival. */
    WAITING,

    /** Ambos jugadores presentes, partida en curso. */
    IN_PROGRESS,

    /** La partida terminó de forma normal (`GAME_OVER`). */
    FINISHED,

    /** Un jugador abandonó / se desconectó antes de terminar. */
    ABANDONED,
}

/**
 * Rol (asiento) del dispositivo local dentro de una partida online. El [index]
 * coincide con el [PlayerId] que controla ese asiento: el anfitrión ([HOST]) es
 * el asiento `0` y los invitados ocupan los asientos siguientes según se van
 * uniendo. Admite hasta [MAX_ONLINE_PLAYERS] asientos (2 o 4 jugadores).
 */
@Serializable
@JvmInline
value class PlayerSlot(val index: Int) {
    init {
        require(index in 0 until MAX_ONLINE_PLAYERS) { "Player slot out of range: $index" }
    }

    /** [PlayerId] de dominio controlado por este asiento. */
    val playerId: PlayerId get() = PlayerId(index)

    companion object {
        /** Anfitrión de la sala; controla `PlayerId(0)`. */
        val HOST = PlayerSlot(0)

        /** Primer invitado; controla `PlayerId(1)`. */
        val GUEST = PlayerSlot(1)
    }
}

/**
 * Estado sincronizado de una partida online. Es el objeto que viaja por el
 * repositorio: transporta el [GameState] (fuente de verdad del tablero) más los
 * metadatos de sala. [version] es un contador monotónico de jugadas aplicadas,
 * usado para ordenar actualizaciones y descartar escrituras obsoletas.
 *
 * [playerNames] y [presence] están indexados por asiento ([PlayerSlot.index]):
 * el elemento `i` corresponde al jugador que ocupa el asiento `i`. Ambas listas
 * crecen a medida que los jugadores se unen. La partida arranca
 * ([MatchStatus.IN_PROGRESS]) cuando se ocupan todos los asientos de
 * [GameConfig.playerCount].
 */
@Serializable
data class OnlineMatch(
    val id: MatchId,
    val config: GameConfig,
    val status: MatchStatus,
    val state: GameState,
    val version: Long,
    val playerNames: List<String> = emptyList(),
    val presence: List<Boolean> = emptyList(),
) {
    /** Cantidad de asientos ya ocupados. */
    val joinedCount: Int get() = playerNames.size

    /** `true` cuando ya se unieron todos los jugadores de la partida. */
    val isFull: Boolean get() = joinedCount >= config.playerCount
}
