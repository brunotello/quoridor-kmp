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
    val playerNames: List<String> = emptyList(),
    val localPlayerId: PlayerId? = null,
    val turnBanner: TurnBanner? = null,
)

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
