package com.btello.quoridor.presentation.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel de la partida. Contiene únicamente la lógica del tablero:
 * turnos, movimientos de peón, colocación de muros y fin de juego.
 *
 * La selección de configuración ("iniciar juego") vive en el feature `menu`.
 */
internal class GameViewModel(config: GameConfig) : ViewModel() {

    private var gameState: GameState = QuoridorRules.startGame(config)
    private var showingWallTargets: Boolean = false
    private var legalWallTargets: List<Wall> = emptyList()
    private var feedback: GameFeedback? = null

    var uiState by mutableStateOf(buildUiState())
        private set

    private val _sideEffects = Channel<GameSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<GameSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.ActivePawnClick -> onActivePawnClick()
            GameEvent.WallReserveClick -> onWallReserveClick()
            is GameEvent.CellClick -> onCellClick(event.cell)
            is GameEvent.WallClick -> onWallClick(event.wall)
            GameEvent.NewGame -> _sideEffects.trySend(GameSideEffect.NavigateToMenu)
        }
    }

    private fun onActivePawnClick() {
        showingWallTargets = false
        legalWallTargets = emptyList()
        refresh()
    }

    private fun onWallReserveClick() {
        val activePlayer = gameState.players.first { it.id == gameState.turn.playerId }
        if (activePlayer.wallsRemaining <= 0) {
            feedback = GameFeedback.NoWallsRemaining
            refresh()
            return
        }
        legalWallTargets = QuoridorRules.getLegalMoves(gameState)
            .filterIsInstance<Move.PlaceWall>()
            .map { it.wall }
        showingWallTargets = legalWallTargets.isNotEmpty()
        if (!showingWallTargets) {
            feedback = GameFeedback.NoLegalWalls
        }
        refresh()
    }

    private fun onCellClick(cell: Cell) {
        val legalPawnMove = QuoridorRules.getLegalMoves(gameState)
            .filterIsInstance<Move.PawnMove>()
            .firstOrNull { it.to == cell }
        if (legalPawnMove != null) {
            applyMove(legalPawnMove)
        }
    }

    private fun onWallClick(wall: Wall) {
        if (!showingWallTargets) return
        val wallMove = QuoridorRules.getLegalMoves(gameState)
            .filterIsInstance<Move.PlaceWall>()
            .firstOrNull { it.wall == wall }
        if (wallMove == null) {
            feedback = GameFeedback.InvalidWall
            refresh()
            return
        }
        applyMove(wallMove)
    }

    private fun applyMove(move: Move) {
        val result = QuoridorRules.applyMove(gameState, move)
        gameState = result.state ?: gameState

        if (!result.isSuccessful || result.error != null) {
            feedback = result.error?.message?.let(GameFeedback::DomainMessage) ?: GameFeedback.InvalidMove
            refresh()
            return
        }

        showingWallTargets = false
        legalWallTargets = emptyList()
        feedback = if (QuoridorRules.isGameOver(gameState)) GameFeedback.GameOver else null
        refresh()
    }

    private fun refresh() {
        uiState = buildUiState()
    }

    private fun buildUiState(): GameUiState {
        val isGameOver = QuoridorRules.isGameOver(gameState)
        val legalTargets = if (isGameOver) {
            emptySet()
        } else {
            QuoridorRules.getLegalMoves(gameState)
                .filterIsInstance<Move.PawnMove>()
                .map { it.to }
                .toSet()
        }
        return GameUiState(
            gameState = gameState,
            legalTargets = legalTargets,
            legalWalls = legalWallTargets.toSet(),
            feedback = feedback,
            isGameOver = isGameOver,
            winnerNumber = gameState.winner?.value?.plus(1),
        )
    }
}
