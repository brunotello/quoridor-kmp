package com.btello.quoridor.presentation.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btello.quoridor.domain.ai.AiStrategy
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_AI_MOVE_DELAY_MS = 450L

/**
 * ViewModel de la partida. Contiene la lógica del tablero (turnos, movimientos de
 * peón, colocación de muros y fin de juego) y, en el modo contra la IA, orquesta
 * los turnos del oponente controlado por la máquina.
 *
 * La selección de configuración ("iniciar juego") vive en el feature `menu`.
 * Los parámetros de IA se inyectan para permitir tests deterministas.
 */
internal class GameViewModel(
    setup: GameSetup,
    private val aiStrategy: AiStrategy? = setup.difficulty?.let { AiStrategy.forDifficulty(it) },
    private val aiMoveDelayMillis: Long = DEFAULT_AI_MOVE_DELAY_MS,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val autoRunAi: Boolean = true,
) : ViewModel() {

    private val aiPlayers: Set<PlayerId> = setup.aiPlayers

    private var gameState: GameState = QuoridorRules.startGame(setup.config)
    private var showingWallTargets: Boolean = false
    private var legalWallTargets: List<Wall> = emptyList()
    private var feedback: GameFeedback? = null
    private var isAiThinking: Boolean = false
    private var aiJob: Job? = null

    var uiState by mutableStateOf(buildUiState())
        private set

    private val _sideEffects = Channel<GameSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<GameSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.ActivePawnClick -> if (!isHumanInputBlocked()) onActivePawnClick()
            GameEvent.WallReserveClick -> if (!isHumanInputBlocked()) onWallReserveClick()
            is GameEvent.CellClick -> if (!isHumanInputBlocked()) onCellClick(event.cell)
            is GameEvent.WallClick -> if (!isHumanInputBlocked()) onWallClick(event.wall)
            GameEvent.NewGame -> {
                aiJob?.cancel()
                _sideEffects.trySend(GameSideEffect.NavigateToMenu)
            }
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
            onHumanMoveApplied()
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
        onHumanMoveApplied()
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

    private fun onHumanMoveApplied() {
        if (autoRunAi) startAiTurn()
    }

    /** Lanza (en corrutina) los turnos de la IA mientras le corresponda jugar. */
    private fun startAiTurn() {
        val strategy = aiStrategy ?: return
        if (!aiControlsCurrentTurn()) return

        aiJob?.cancel()
        isAiThinking = true
        refresh()
        aiJob = viewModelScope.launch {
            while (isActive && aiControlsCurrentTurn()) {
                delay(aiMoveDelayMillis)
                val move = withContext(computeDispatcher) {
                    strategy.chooseMove(gameState, gameState.turn.playerId)
                } ?: break
                applyMove(move)
            }
            isAiThinking = false
            refresh()
        }
    }

    /** True si el turno actual pertenece a un jugador controlado por la IA y la partida sigue en curso. */
    internal fun aiControlsCurrentTurn(): Boolean =
        !QuoridorRules.isGameOver(gameState) && gameState.turn.playerId in aiPlayers

    private fun isHumanInputBlocked(): Boolean = isAiThinking || aiControlsCurrentTurn()

    /**
     * Ejecuta de forma síncrona (sin retardo ni corrutinas) los turnos pendientes
     * de la IA. Pensado para tests deterministas; en producción se usa [startAiTurn].
     */
    internal fun runAiTurnsForTest() {
        val strategy = aiStrategy ?: return
        while (aiControlsCurrentTurn()) {
            val move = strategy.chooseMove(gameState, gameState.turn.playerId) ?: break
            applyMove(move)
        }
    }

    private fun refresh() {
        uiState = buildUiState()
    }

    private fun buildUiState(): GameUiState {
        val isGameOver = QuoridorRules.isGameOver(gameState)
        val humanTurn = !isGameOver && gameState.turn.playerId !in aiPlayers
        val legalTargets = if (humanTurn) {
            QuoridorRules.getLegalMoves(gameState)
                .filterIsInstance<Move.PawnMove>()
                .map { it.to }
                .toSet()
        } else {
            emptySet()
        }
        return GameUiState(
            gameState = gameState,
            legalTargets = legalTargets,
            legalWalls = if (humanTurn) legalWallTargets.toSet() else emptySet(),
            feedback = feedback,
            isGameOver = isGameOver,
            isAiThinking = isAiThinking,
            aiPlayers = aiPlayers,
            winnerNumber = gameState.winner?.value?.plus(1),
        )
    }
}
