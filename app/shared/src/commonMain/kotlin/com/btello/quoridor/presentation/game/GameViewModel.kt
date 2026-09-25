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
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.domain.stats.StatisticsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

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
    private val statisticsRepository: StatisticsRepository? = null,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val onlineRepository: OnlineGameRepository? = null,
    onlineScope: CoroutineScope? = null,
) : ViewModel() {

    private val onlineScope: CoroutineScope = onlineScope ?: viewModelScope
    private val onlineSession = setup.online
    private val isOnline = onlineSession != null
    private val localPlayerId = onlineSession?.localPlayerId

    /** Versión (nº de jugada) sincronizada de la partida online. */
    private var onlineVersion: Long = 0
    private var onlineStatus: MatchStatus? = if (isOnline) MatchStatus.WAITING else null
    private var onlinePlayerNames: List<String> = emptyList()
    private var onlineJob: Job? = null

    private val aiPlayers: Set<PlayerId> = setup.aiPlayers
    private val difficulty = setup.difficulty
    private val startMark = timeSource.markNow()

    /** Cantidad de jugadas por jugador, para registrar estadísticas al finalizar. */
    private val moveCounts = mutableMapOf<PlayerId, Int>()

    /** Cantidad de muros colocados por jugador. */
    private val wallCounts = mutableMapOf<PlayerId, Int>()

    private var gameRecorded = false

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

    init {
        startObservingOnline()
    }

    /** Escucha en tiempo real el estado de la sala online y adopta las jugadas del rival. */
    private fun startObservingOnline() {
        val repository = onlineRepository ?: return
        val session = onlineSession ?: return
        onlineJob = onlineScope.launch {
            repository.observeMatch(session.matchId).collect { match ->
                onlineStatus = match.status
                onlinePlayerNames = match.playerNames
                if (match.version > onlineVersion) {
                    onlineVersion = match.version
                    gameState = match.state
                    val gameOver = QuoridorRules.isGameOver(gameState)
                    feedback = if (gameOver) GameFeedback.GameOver else null
                }
                refresh()
            }
        }
    }

    /** Publica el [gameState] local tras una jugada del jugador de este dispositivo. */
    private fun publishOnlineMove() {
        val repository = onlineRepository ?: return
        val session = onlineSession ?: return
        onlineVersion += 1
        val version = onlineVersion
        onlineScope.launch {
            repository.submitMove(session.matchId, gameState, version)
        }
    }

    private fun leaveOnline() {
        val repository = onlineRepository ?: return
        val session = onlineSession ?: return
        onlineScope.launch {
            repository.leaveMatch(session.matchId, session.slot)
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.ActivePawnClick -> if (!isHumanInputBlocked()) onActivePawnClick()
            GameEvent.WallReserveClick -> if (!isHumanInputBlocked()) onWallReserveClick()
            is GameEvent.CellClick -> if (!isHumanInputBlocked()) onCellClick(event.cell)
            is GameEvent.WallClick -> if (!isHumanInputBlocked()) onWallClick(event.wall)
            GameEvent.NewGame -> {
                aiJob?.cancel()
                onlineJob?.cancel()
                leaveOnline()
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

        moveCounts[move.playerId] = (moveCounts[move.playerId] ?: 0) + 1
        if (move is Move.PlaceWall) {
            wallCounts[move.playerId] = (wallCounts[move.playerId] ?: 0) + 1
        }

        showingWallTargets = false
        legalWallTargets = emptyList()
        val gameOver = QuoridorRules.isGameOver(gameState)
        feedback = if (gameOver) GameFeedback.GameOver else null
        if (gameOver) recordGame()
        refresh()
    }

    /**
     * Registra la partida finalizada (una única vez) desde la perspectiva del
     * jugador humano ([PlayerId] 0). Las métricas de jugadas y muros son las del
     * ganador.
     */
    private fun recordGame() {
        if (isOnline) return
        val repository = statisticsRepository ?: return
        if (gameRecorded) return
        val winner = gameState.winner ?: return
        gameRecorded = true
        repository.record(
            GameRecord(
                won = winner == PlayerId(0),
                difficulty = difficulty,
                durationMillis = startMark.elapsedNow().inWholeMilliseconds,
                moveCount = moveCounts[winner] ?: 0,
                wallsUsed = wallCounts[winner] ?: 0,
            ),
        )
    }

    private fun onHumanMoveApplied() {
        if (isOnline) {
            publishOnlineMove()
        } else if (autoRunAi) {
            startAiTurn()
        }
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
                    strategy.chooseMoveAsync(gameState, gameState.turn.playerId)
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

    private fun isHumanInputBlocked(): Boolean =
        isAiThinking || aiControlsCurrentTurn() || isOnlineInputBlocked()

    /** En online se bloquea la entrada salvo que la sala esté en curso y sea el turno local. */
    private fun isOnlineInputBlocked(): Boolean =
        isOnline && (onlineStatus != MatchStatus.IN_PROGRESS || gameState.turn.playerId != localPlayerId)

    /** True si la partida online terminó porque un jugador la abandonó. */
    private fun isOnlineAbandoned(): Boolean =
        isOnline && onlineStatus == MatchStatus.ABANDONED

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
        val isAbandoned = isOnlineAbandoned()
        val isGameOver = QuoridorRules.isGameOver(gameState) || isAbandoned
        val humanTurn = !isGameOver && gameState.turn.playerId !in aiPlayers && !isOnlineInputBlocked()
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
            feedback = feedback ?: onlineFeedback(isGameOver),
            isGameOver = isGameOver,
            isAbandoned = isAbandoned,
            isAiThinking = isAiThinking,
            aiPlayers = aiPlayers,
            winnerNumber = gameState.winner?.value?.plus(1),
            playerNames = if (isOnline) onlinePlayerNames else emptyList(),
            localPlayerId = localPlayerId,
            turnBanner = turnBanner(isGameOver),
        )
    }

    /** Indicador de turno online mostrado sobre el tablero (sólo con la partida en curso). */
    private fun turnBanner(isGameOver: Boolean): TurnBanner? {
        if (!isOnline || isGameOver || onlineStatus != MatchStatus.IN_PROGRESS) return null
        val turnId = gameState.turn.playerId
        return if (turnId == localPlayerId) {
            TurnBanner.YourTurn
        } else {
            TurnBanner.PlayerTurn(
                playerNumber = turnId.value + 1,
                playerName = onlinePlayerNames.getOrNull(turnId.value)?.takeIf { it.isNotBlank() },
            )
        }
    }

    /** Feedback derivado del estado online (sala esperando o abandono). */
    private fun onlineFeedback(isGameOver: Boolean): GameFeedback? = when {
        !isOnline || isGameOver -> null
        onlineStatus == MatchStatus.ABANDONED -> GameFeedback.OpponentLeft
        onlineStatus != MatchStatus.IN_PROGRESS -> GameFeedback.WaitingOpponent
        else -> null
    }
}
