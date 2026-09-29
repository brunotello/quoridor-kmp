package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.online.SeriesFormat

/**
 * Estado de UI de la partida en curso (feature `game`).
 *
 * En modo online, [playerNames] contiene el nombre sincronizado de cada jugador
 * (indexado por [PlayerId.value]), [localPlayerId] identifica al jugador de este
 * dispositivo y [turnBanner] describe el indicador de turno mostrado sobre el
 * tablero.
 */
internal data class GameUiState(
    val gameState: GameState,
    val legalTargets: Set<Cell> = emptySet(),
    val legalWalls: Set<Wall> = emptySet(),
    val feedback: GameFeedback? = null,
    val isGameOver: Boolean = false,
    val isAbandoned: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiPlayers: Set<PlayerId> = emptySet(),
    val winnerNumber: Int? = null,
    val localResult: GameResult? = null,
    val playerNames: List<String> = emptyList(),
    val localPlayerId: PlayerId? = null,
    val turnBanner: TurnBanner? = null,
    val competitive: CompetitiveUi? = null,
    val isSeriesOver: Boolean = true,
    val matchIntro: MatchIntro? = null,
)

/**
 * Estado de la introducción de una partida online, mostrado como título centrado
 * sobre el tablero (que permanece deshabilitado mientras está presente).
 * La capa Compose resuelve cada caso a texto de `strings.xml`.
 */
internal sealed interface MatchIntro {
    /** La sala espera a que se conecten todos los jugadores. */
    data object WaitingForPlayers : MatchIntro

    /**
     * Cuenta atrás previa al inicio del juego. [value] va de 5 a 0; el 0 anuncia
     * el comienzo del juego ("¡comienza el juego!") en lugar de mostrar el número.
     */
    data class Countdown(val value: Int) : MatchIntro
}

/**
 * Información del modo competitivo mostrada sobre el tablero: el formato de la
 * serie ([format]), las victorias por jugador ([wins], indexado por
 * [PlayerId.value]), cuántas hacen falta para ganarla ([gamesToWin]) y el tiempo
 * restante de cada jugador en milisegundos ([clocksMillis], `null` si la partida
 * no tiene temporizador). [localPlayerId] identifica al jugador de este
 * dispositivo para resaltarlo. [presentPlayerIds] son los [PlayerId.value] que
 * siguen en la serie: quienes abandonaron se ocultan del marcador, pero quien
 * pierde un juego por tiempo permanece porque continúa en el siguiente juego.
 */
internal data class CompetitiveUi(
    val format: SeriesFormat,
    val wins: List<Int>,
    val gamesToWin: Int,
    val clocksMillis: List<Long>? = null,
    val localPlayerId: PlayerId? = null,
    val presentPlayerIds: Set<Int> = wins.indices.toSet(),
)

/**
 * Resultado de la partida desde la perspectiva del jugador de este dispositivo.
 * Es `null` cuando no hay una perspectiva local (p. ej. partida local a dos
 * jugadores en el mismo dispositivo), en cuyo caso sólo se anuncia al ganador.
 */
internal enum class GameResult {
    /** El jugador local ganó la partida. */
    WON,

    /** El jugador local perdió la partida. */
    LOST,
}

/**
 * Indicador de turno de una partida online, mostrado como texto sobre el tablero.
 * La capa Compose resuelve cada caso a texto de `strings.xml`.
 */
internal sealed interface TurnBanner {
    /** Es el turno del jugador de este dispositivo. */
    data object YourTurn : TurnBanner

    /** Es el turno de otro jugador; se muestra su [playerName] o "Jugador N". */
    data class PlayerTurn(val playerNumber: Int, val playerName: String?) : TurnBanner
}
