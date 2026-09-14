package com.btello.quoridor.presentation.game

import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall

/**
 * Estado de UI de la partida en curso (feature `game`).
 */
internal data class GameUiState(
    val gameState: GameState,
    val legalTargets: Set<Cell> = emptySet(),
    val legalWalls: Set<Wall> = emptySet(),
    val feedback: GameFeedback? = null,
    val isGameOver: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiPlayers: Set<PlayerId> = emptySet(),
    val winnerNumber: Int? = null,
)
