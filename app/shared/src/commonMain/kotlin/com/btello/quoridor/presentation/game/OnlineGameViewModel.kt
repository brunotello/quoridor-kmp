package com.btello.quoridor.presentation.game

import androidx.lifecycle.viewModelScope
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.CompetitiveState
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Frecuencia de refresco del temporizador estilo ajedrez (ms). */
private const val CLOCK_TICK_MILLIS = 250L

/** Valor inicial de la cuenta atrás previa al inicio del juego (5, 4, …, 0). */
private const val COUNTDOWN_START = 5

/** Duración de cada paso de la cuenta atrás previa al inicio (ms). */
private const val COUNTDOWN_STEP_MILLIS = 1000L

/**
 * ViewModel de una partida online. Hereda de [GameViewModel] para reutilizar
 * intacta la lógica de juego (turnos, jugadas, muros, fin de partida) y añade la
 * sincronización con la sala remota: adopta las jugadas del rival, publica las
 * propias, gestiona el abandono y expone el estado online (nombres, indicador de
 * turno, feedback de espera/abandono).
 *
 * También orquesta el **modo competitivo**: la serie al mejor de N (varios juegos
 * hasta que alguien alcance las victorias necesarias) y el temporizador estilo
 * ajedrez (cada jugador gasta su reloj en su turno; si se agota, pierde el juego
 * en curso). Ambos son opcionales y viajan en [CompetitiveState] dentro de la
 * sala sincronizada.
 *
 * Requiere una [GameSetup.online]. El [onlineRepository] puede ser `null` en
 * plataformas sin backend online, en cuyo caso no se sincroniza.
 */
internal class OnlineGameViewModel(
    setup: GameSetup,
    private val onlineRepository: OnlineGameRepository? = null,
    onlineScope: CoroutineScope? = null,
    autoRunAi: Boolean = true,
    private val clockTimeSource: TimeSource = TimeSource.Monotonic,
) : GameViewModel(
    setup = setup,
    autoRunAi = autoRunAi,
) {

    private val session = requireNotNull(setup.online) {
        "OnlineGameViewModel requiere una GameSetup con sesión online"
    }
    private val gameConfig: GameConfig = setup.config
    private val localPlayerId: PlayerId = session.localPlayerId
    private val onlineScope: CoroutineScope = onlineScope ?: viewModelScope

    /** Versión (nº de jugada) sincronizada de la partida online. */
    private var onlineVersion: Long = 0
    private var onlineStatus: MatchStatus = MatchStatus.WAITING
    private var initialized: Boolean = false

    /**
     * Nullable a propósito: el constructor base construye el estado inicial de UI
     * (que invoca [playerNames]) antes de que se inicialicen los campos de esta
     * subclase, por lo que la lectura temprana devuelve `null` y se resuelve a
     * lista vacía.
     */
    private var onlinePlayerNames: List<String>? = emptyList()

    /**
     * Presencia por asiento sincronizada: `presence[i]` es `false` cuando el
     * jugador del asiento `i` abandonó/se desconectó. Se usa para decidir quién
     * sigue en la serie: un jugador que pierde un juego por tiempo sigue presente
     * (`true`) y debe seguir viéndose en el marcador, a diferencia de quien
     * abandona.
     */
    private var onlinePresence: List<Boolean>? = emptyList()

    /** Estado competitivo (serie y relojes) sincronizado; por defecto sin efecto. */
    private var competitiveState: CompetitiveState? = CompetitiveState()

    /**
     * Acceso no nulo al estado competitivo. Es nullable a propósito: el constructor
     * base construye el estado de UI (que invoca [competitiveUi]/[seriesOver]) antes
     * de inicializar los campos de esta subclase, por lo que la lectura temprana
     * cae en el valor por defecto sin efecto competitivo.
     */
    private val competitive: CompetitiveState
        get() = competitiveState ?: CompetitiveState()

    /** Marca del inicio del turno actual, base para descontar el reloj del jugador activo. */
    private var turnStartMark: TimeMark? = null

    private var onlineJob: Job? = null
    private var tickJob: Job? = null

    /**
     * Cuenta atrás previa al inicio del juego (5 → 0) una vez que se unieron todos
     * los jugadores; `null` cuando no está activa. Bloquea la entrada mientras corre.
     */
    private var countdownValue: Int? = null

    /** `true` una vez lanzada la cuenta atrás inicial, para no repetirla. */
    private var introStarted: Boolean = false

    private var countdownJob: Job? = null

    override val localHumanId: PlayerId? = localPlayerId

    init {
        startObservingOnline()
        refresh()
    }

    /** Escucha en tiempo real el estado de la sala online y adopta las jugadas del rival. */
    private fun startObservingOnline() {
        val repository = onlineRepository ?: return
        onlineJob = onlineScope.launch {
            repository.observeMatch(session.matchId).collect { match ->
                onlineStatus = match.status
                onlinePlayerNames = match.playerNames
                onlinePresence = match.presence
                if (!initialized || match.version > onlineVersion) {
                    initialized = true
                    onlineVersion = match.version
                    gameState = match.state
                    competitiveState = match.competitive
                    val gameOver = QuoridorRules.isGameOver(gameState)
                    feedback = if (gameOver) GameFeedback.GameOver else null
                    resetTurnMark()
                    if (gameOver) stopTicking() else startTicking()
                }
                if (onlineStatus == MatchStatus.IN_PROGRESS) startIntroCountdown()
                refresh()
            }
        }
    }

    /**
     * Lanza (una única vez) la cuenta atrás previa al inicio del juego cuando ya se
     * unieron todos los jugadores: muestra 5, 4, …, 0 (el 0 anuncia el comienzo) y
     * mantiene la entrada bloqueada hasta terminar.
     */
    private fun startIntroCountdown() {
        if (introStarted) return
        introStarted = true
        countdownJob = onlineScope.launch {
            for (value in COUNTDOWN_START downTo 0) {
                countdownValue = value
                refresh()
                delay(COUNTDOWN_STEP_MILLIS)
            }
            countdownValue = null
            resetTurnMark()
            refresh()
        }
    }

    // --- Publicación de estado ---

    /** Publica el [gameState] y el marcador competitivo actuales con la nueva versión. */
    private fun publish() {
        val repository = onlineRepository ?: return
        val state = gameState
        val comp = competitive
        val version = onlineVersion
        onlineScope.launch { repository.submitMove(session.matchId, state, comp, version) }
    }

    private fun leaveOnline() {
        val repository = onlineRepository ?: return
        onlineScope.launch { repository.leaveMatch(session.matchId, session.slot) }
    }

    override fun onHumanMoveApplied() {
        competitiveState = competitive.copy(remainingMillis = frozenLocalClock())
        if (QuoridorRules.isGameOver(gameState)) {
            finishGameAndPublish()
        } else {
            onlineVersion += 1
            publish()
            resetTurnMark()
        }
    }

    /**
     * Cierra el juego en curso: suma la victoria al ganador y publica el estado.
     * Si con esa victoria se decide la serie, la sala pasará a `FINISHED`; si no,
     * ambos verán el marcador y el ganador podrá continuar al siguiente juego.
     */
    private fun finishGameAndPublish() {
        gameState.winner?.let { winner -> competitiveState = competitive.copy(wins = incrementedWins(winner)) }
        feedback = GameFeedback.GameOver
        onlineVersion += 1
        publish()
        resetTurnMark()
        stopTicking()
    }

    override fun onContinueSeries() {
        if (competitive.isSeriesOver || isAbandoned()) {
            onNewGame()
            return
        }
        // Sólo el ganador del último juego inicia el siguiente; el rival espera la sincronización.
        if (gameState.winner != localPlayerId) return
        gameState = QuoridorRules.startGame(gameConfig)
        competitiveState = competitive.copy(
            gameIndex = competitive.gameIndex + 1,
            remainingMillis = initialClocks(),
        )
        feedback = null
        onlineVersion += 1
        publish()
        resetTurnMark()
        startTicking()
        refresh()
    }

    override fun onNewGame() {
        aiJob?.cancel()
        onlineJob?.cancel()
        countdownJob?.cancel()
        stopTicking()
        leaveOnline()
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
    }

    /**
     * El jugador local abandona la partida. Se lo quita del estado con
     * [QuoridorRules.withPlayerRemoved] y se publica el nuevo estado: si sólo queda
     * un rival (1v1), este se lleva la serie y la partida termina; si quedan dos o
     * más (4 jugadores), la partida sigue sin el que se fue.
     */
    override fun onLeaveMatch() {
        aiJob?.cancel()
        onlineJob?.cancel()
        countdownJob?.cancel()
        stopTicking()
        if (!QuoridorRules.isGameOver(gameState)) {
            gameState = QuoridorRules.withPlayerRemoved(gameState, localPlayerId)
            competitiveState = competitive.copy(remainingMillis = frozenLocalClock())
            gameState.winner?.let { winner ->
                competitiveState = competitive.copy(wins = seriesForfeitedTo(winner))
                feedback = GameFeedback.GameOver
            }
            onlineVersion += 1
            publish()
        }
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
    }

    /** Online no registra estadísticas locales. */
    override fun recordGame() = Unit

    // --- Temporizador estilo ajedrez ---

    private fun startTicking() {
        if (!competitive.config.hasTimer) return
        if (tickJob?.isActive == true) return
        tickJob = onlineScope.launch {
            while (isActive) {
                delay(CLOCK_TICK_MILLIS)
                checkLocalTimeout()
                refresh()
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    /** Si es mi turno y mi reloj llegó a cero, pierdo el juego en curso. */
    private fun checkLocalTimeout() {
        if (!competitive.config.hasTimer) return
        if (onlineStatus != MatchStatus.IN_PROGRESS) return
        if (countdownValue != null) return
        if (QuoridorRules.isGameOver(gameState)) return
        if (gameState.turn.playerId != localPlayerId) return
        val mine = remainingNow().getOrNull(localPlayerId.value) ?: return
        if (mine <= 0L) handleLocalTimeout()
    }

    private fun handleLocalTimeout() {
        val zeroed = competitive.remainingMillis.toMutableList()
        if (localPlayerId.value < zeroed.size) zeroed[localPlayerId.value] = 0L
        gameState = QuoridorRules.withPlayerRemoved(gameState, localPlayerId)
        competitiveState = competitive.copy(remainingMillis = zeroed)
        finishGameAndPublish()
        refresh()
    }

    /** Tiempos restantes "ahora", descontando lo transcurrido del turno del jugador activo. */
    private fun remainingNow(): List<Long> {
        val base = competitive.remainingMillis
        if (base.isEmpty()) return base
        val running = onlineStatus == MatchStatus.IN_PROGRESS &&
            countdownValue == null &&
            !QuoridorRules.isGameOver(gameState)
        if (!running) return base
        val active = gameState.turn.playerId.value
        val elapsed = turnStartMark?.elapsedNow()?.inWholeMilliseconds ?: 0L
        return base.mapIndexed { index, millis ->
            if (index == active) (millis - elapsed).coerceAtLeast(0L) else millis
        }
    }

    /** Relojes con el tiempo consumido por el jugador local ya descontado. */
    private fun frozenLocalClock(): List<Long> {
        val base = competitive.remainingMillis
        if (base.isEmpty()) return base
        val elapsed = turnStartMark?.elapsedNow()?.inWholeMilliseconds ?: 0L
        return base.mapIndexed { index, millis ->
            if (index == localPlayerId.value) (millis - elapsed).coerceAtLeast(0L) else millis
        }
    }

    private fun initialClocks(): List<Long> =
        competitive.config.timeControlSeconds
            ?.let { seconds -> List(gameConfig.playerCount) { seconds * 1000L } }
            ?: emptyList()

    private fun incrementedWins(winner: PlayerId): List<Int> =
        competitive.wins.toMutableList().also {
            if (winner.value < it.size) it[winner.value] += 1
        }

    private fun seriesForfeitedTo(winner: PlayerId): List<Int> =
        competitive.wins.toMutableList().also {
            if (winner.value < it.size) it[winner.value] = competitive.config.format.gamesToWin
        }

    private fun resetTurnMark() {
        turnStartMark = clockTimeSource.markNow()
    }

    // --- Puntos de extensión del estado de UI ---

    override fun isInputBlocked(): Boolean = super.isInputBlocked() || isOnlineInputBlocked()

    override fun isRemoteInputBlocked(): Boolean = isOnlineInputBlocked()

    /** Se bloquea la entrada salvo que la sala esté en curso, sin cuenta atrás y sea el turno local. */
    private fun isOnlineInputBlocked(): Boolean =
        onlineStatus != MatchStatus.IN_PROGRESS || countdownValue != null || gameState.turn.playerId != localPlayerId

    override fun isAbandoned(): Boolean = onlineStatus == MatchStatus.ABANDONED

    override fun extraFeedback(isGameOver: Boolean): GameFeedback? = when {
        isGameOver -> null
        onlineStatus == MatchStatus.ABANDONED -> GameFeedback.OpponentLeft
        onlineStatus != MatchStatus.IN_PROGRESS -> GameFeedback.WaitingOpponent
        else -> null
    }

    override fun playerNames(): List<String> = onlinePlayerNames.orEmpty()

    override fun localPlayerIdOrNull(): PlayerId? = localPlayerId

    /** Indicador de turno online mostrado sobre el tablero (sólo con la partida en curso). */
    override fun turnBanner(isGameOver: Boolean): TurnBanner? {
        if (isGameOver || onlineStatus != MatchStatus.IN_PROGRESS) return null
        val turnId = gameState.turn.playerId
        return if (turnId == localPlayerId) {
            TurnBanner.YourTurn
        } else {
            TurnBanner.PlayerTurn(
                playerNumber = turnId.value + 1,
                playerName = onlinePlayerNames.orEmpty().getOrNull(turnId.value)?.takeIf { it.isNotBlank() },
            )
        }
    }

    override fun competitiveUi(): CompetitiveUi? {
        if (!competitive.config.isCompetitive) return null
        return CompetitiveUi(
            format = competitive.config.format,
            wins = competitive.wins,
            gamesToWin = competitive.config.format.gamesToWin,
            clocksMillis = if (competitive.config.hasTimer) remainingNow() else null,
            localPlayerId = localPlayerId,
            presentPlayerIds = presentPlayerIds(),
        )
    }

    /**
     * Asientos que siguen en la serie: los que no abandonaron según [onlinePresence].
     * Un jugador que perdió el juego en curso por tiempo sigue presente, por lo que
     * continúa viéndose en el marcador; sólo se ocultan quienes abandonaron. Si aún
     * no hay datos de presencia, se cae en los jugadores presentes en el tablero.
     */
    private fun presentPlayerIds(): Set<Int> {
        val presence = onlinePresence.orEmpty()
        if (presence.isEmpty()) return gameState.players.map { it.id.value }.toSet()
        return presence.indices.filter { presence[it] }.toSet()
    }

    override fun seriesOver(): Boolean = isAbandoned() || competitive.isSeriesOver

    /**
     * Introducción de la partida mostrada sobre el tablero: mientras la sala espera
     * jugadores, un aviso de espera; una vez completa, la cuenta atrás previa al
     * inicio (5 → 0). `null` cuando el juego ya está en marcha o terminó.
     */
    override fun matchIntro(): MatchIntro? {
        if (onlineStatus == MatchStatus.WAITING) return MatchIntro.WaitingForPlayers
        return countdownValue?.let { MatchIntro.Countdown(it) }
    }
}
