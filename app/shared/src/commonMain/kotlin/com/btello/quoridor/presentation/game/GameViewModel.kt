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
import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.domain.stats.StatisticsRepository
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
import kotlin.time.TimeSource

private const val DEFAULT_AI_MOVE_DELAY_MS = 450L

/**
 * ViewModel base de la partida (modo local: 1 contra 1 en el mismo dispositivo o
 * contra la IA). Contiene toda la lógica del tablero (turnos, movimientos de
 * peón, colocación de muros y fin de juego) y orquesta los turnos del oponente
 * controlado por la máquina.
 *
 * No conoce nada del modo online: esa funcionalidad la aporta
 * [OnlineGameViewModel], que hereda de esta clase y reutiliza la lógica de juego
 * como base, sobrescribiendo únicamente los puntos de extensión (`protected open`)
 * necesarios para sincronizar la sala.
 *
 * La selección de configuración ("iniciar juego") vive en el feature `menu`.
 * Los parámetros de IA se inyectan para permitir tests deterministas.
 */
internal open class GameViewModel(
    setup: GameSetup,
    private val aiStrategy: AiStrategy? = setup.difficulty?.let { AiStrategy.forDifficulty(it) },
    private val aiMoveDelayMillis: Long = DEFAULT_AI_MOVE_DELAY_MS,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val autoRunAi: Boolean = true,
    private val statisticsRepository: StatisticsRepository? = null,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    /**
     * Jugador que controla este dispositivo, usado para decidir si la partida se
     * ganó o se perdió. Contra IA es el único humano; en partida local compartida
     * no hay perspectiva local (`null`). El modo online la redefine con el asiento
     * local.
     */
    protected open val localHumanId: PlayerId? =
        (0 until setup.config.playerCount)
            .map { PlayerId(it) }
            .filterNot { it in setup.aiPlayers }
            .singleOrNull()

    protected val aiPlayers: Set<PlayerId> = setup.aiPlayers
    private val difficulty = setup.difficulty
    private val startMark = timeSource.markNow()

    /** Cantidad de jugadas por jugador, para registrar estadísticas al finalizar. */
    private val moveCounts = mutableMapOf<PlayerId, Int>()

    /** Cantidad de muros colocados por jugador. */
    private val wallCounts = mutableMapOf<PlayerId, Int>()

    private var gameRecorded = false

    protected var gameState: GameState = QuoridorRules.startGame(setup.config)
    private var showingWallTargets: Boolean = false
    private var legalWallTargets: List<Wall> = emptyList()
    protected var feedback: GameFeedback? = null
    private var isAiThinking: Boolean = false
    protected var aiJob: Job? = null

    var uiState by mutableStateOf(buildUiState())
        private set

    protected val _sideEffects = Channel<GameSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<GameSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.ActivePawnClick -> if (!isInputBlocked()) onActivePawnClick()
            GameEvent.WallReserveClick -> if (!isInputBlocked()) onWallReserveClick()
            is GameEvent.CellClick -> if (!isInputBlocked()) onCellClick(event.cell)
            is GameEvent.WallClick -> if (!isInputBlocked()) onWallClick(event.wall)
            GameEvent.LeaveMatch -> onLeaveMatch()
            GameEvent.NewGame -> onNewGame()
            GameEvent.ContinueSeries -> onContinueSeries()
        }
    }

    /** Volver al menú para iniciar otra partida. */
    protected open fun onNewGame() {
        aiJob?.cancel()
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
    }

    /** Avanzar al siguiente juego de una serie competitiva. Sin efecto en local. */
    protected open fun onContinueSeries() = onNewGame()

    /**
     * El jugador abandona la partida. En local sólo se sale al menú sin
     * consecuencias en el estado de juego.
     */
    protected open fun onLeaveMatch() {
        aiJob?.cancel()
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
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
     * ganador. El modo online no registra estadísticas.
     */
    protected open fun recordGame() {
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

    /**
     * Acción tras aplicar una jugada del jugador de este dispositivo. En local,
     * cede el turno a la IA cuando corresponde. El modo online publica la jugada.
     */
    protected open fun onHumanMoveApplied() {
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

    /** True si la entrada del usuario debe ignorarse en el estado actual. */
    protected open fun isInputBlocked(): Boolean =
        isAiThinking || aiControlsCurrentTurn()

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

    protected fun refresh() {
        uiState = buildUiState()
    }

    private fun buildUiState(): GameUiState {
        val isAbandoned = isAbandoned()
        val isGameOver = QuoridorRules.isGameOver(gameState) || isAbandoned
        val humanTurn = !isGameOver && gameState.turn.playerId !in aiPlayers && !isRemoteInputBlocked()
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
            feedback = feedback ?: extraFeedback(isGameOver),
            isGameOver = isGameOver,
            isAbandoned = isAbandoned,
            isAiThinking = isAiThinking,
            aiPlayers = aiPlayers,
            winnerNumber = gameState.winner?.value?.plus(1),
            localResult = localResult(isAbandoned),
            playerNames = playerNames(),
            localPlayerId = localPlayerIdOrNull(),
            turnBanner = turnBanner(isGameOver),
            competitive = competitiveUi(),
            isSeriesOver = if (isGameOver) seriesOver() else true,
            matchIntro = matchIntro(),
        )
    }

    /**
     * Resultado de la partida desde la perspectiva del jugador local. Es `null`
     * si no hay un jugador local (partida local compartida) o si la partida sigue
     * en curso. Un abandono del rival cuenta como victoria local.
     */
    private fun localResult(isAbandoned: Boolean): GameResult? {
        val localId = localHumanId ?: return null
        if (isAbandoned) return GameResult.WON
        val winner = gameState.winner ?: return null
        return if (winner == localId) GameResult.WON else GameResult.LOST
    }

    // --- Puntos de extensión para el modo online (sin efecto en local) ---

    /** True si la partida terminó porque un jugador la abandonó. */
    protected open fun isAbandoned(): Boolean = false

    /** True si la entrada local está bloqueada por el estado remoto (turno del rival, sala en espera). */
    protected open fun isRemoteInputBlocked(): Boolean = false

    /** Feedback adicional derivado del estado remoto (sala esperando o abandono). */
    protected open fun extraFeedback(isGameOver: Boolean): GameFeedback? = null

    /** Nombres sincronizados de los jugadores (indexados por [PlayerId.value]); vacío en local. */
    protected open fun playerNames(): List<String> = emptyList()

    /** Identificador del jugador de este dispositivo; `null` en local. */
    protected open fun localPlayerIdOrNull(): PlayerId? = null

    /** Indicador de turno mostrado sobre el tablero; `null` en local. */
    protected open fun turnBanner(isGameOver: Boolean): TurnBanner? = null

    /** Información del modo competitivo (serie/reloj); `null` fuera del online competitivo. */
    protected open fun competitiveUi(): CompetitiveUi? = null

    /**
     * `true` cuando el fin del juego actual también cierra la partida (serie
     * decidida o abandono). En local siempre es `true`: cada juego es la partida.
     */
    protected open fun seriesOver(): Boolean = true

    /** Introducción de la partida (espera de jugadores / cuenta atrás); `null` en local. */
    protected open fun matchIntro(): MatchIntro? = null
}
