package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall

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
