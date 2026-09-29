package com.btello.quoridor.presentation.game

import androidx.lifecycle.viewModelScope
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel de una partida online. Hereda de [GameViewModel] para reutilizar
 * intacta la lógica de juego (turnos, jugadas, muros, fin de partida) y sólo
 * añade la sincronización con la sala remota: adopta las jugadas del rival,
 * publica las propias, gestiona el abandono y expone el estado online (nombres,
 * indicador de turno, feedback de espera/abandono).
 *
 * Requiere una [GameSetup.online]. El [onlineRepository] puede ser `null` en
 * plataformas sin backend online, en cuyo caso no se sincroniza.
 */
internal class OnlineGameViewModel(
    setup: GameSetup,
    private val onlineRepository: OnlineGameRepository? = null,
    onlineScope: CoroutineScope? = null,
    autoRunAi: Boolean = true,
) : GameViewModel(
    setup = setup,
    autoRunAi = autoRunAi,
) {

    private val session = requireNotNull(setup.online) {
        "OnlineGameViewModel requiere una GameSetup con sesión online"
    }
    private val localPlayerId: PlayerId = session.localPlayerId
    private val onlineScope: CoroutineScope = onlineScope ?: viewModelScope

    /** Versión (nº de jugada) sincronizada de la partida online. */
    private var onlineVersion: Long = 0
    private var onlineStatus: MatchStatus = MatchStatus.WAITING

    /**
     * Nullable a propósito: el constructor base construye el estado inicial de UI
     * (que invoca [playerNames]) antes de que se inicialicen los campos de esta
     * subclase, por lo que la lectura temprana devuelve `null` y se resuelve a
     * lista vacía.
     */
    private var onlinePlayerNames: List<String>? = emptyList()
    private var onlineJob: Job? = null

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
        onlineVersion += 1
        val version = onlineVersion
        onlineScope.launch {
            repository.submitMove(session.matchId, gameState, version)
        }
    }

    private fun leaveOnline() {
        val repository = onlineRepository ?: return
        onlineScope.launch {
            repository.leaveMatch(session.matchId, session.slot)
        }
    }

    override fun onHumanMoveApplied() {
        publishOnlineMove()
    }

    override fun onNewGame() {
        aiJob?.cancel()
        onlineJob?.cancel()
        leaveOnline()
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
    }

    /**
     * El jugador local abandona la partida. Se lo quita del estado con
     * [QuoridorRules.withPlayerRemoved] y se publica el nuevo estado para el resto:
     * si sólo queda un rival (1v1), este gana y la partida termina; si quedan dos o
     * más (4 jugadores), la partida sigue sin el que se fue.
     */
    override fun onLeaveMatch() {
        aiJob?.cancel()
        onlineJob?.cancel()
        if (!QuoridorRules.isGameOver(gameState)) {
            gameState = QuoridorRules.withPlayerRemoved(gameState, localPlayerId)
            if (QuoridorRules.isGameOver(gameState)) {
                feedback = GameFeedback.GameOver
            }
            publishOnlineMove()
        }
        _sideEffects.trySend(GameSideEffect.NavigateToMenu)
    }

    /** Online no registra estadísticas locales. */
    override fun recordGame() = Unit

    override fun isInputBlocked(): Boolean = super.isInputBlocked() || isOnlineInputBlocked()

    override fun isRemoteInputBlocked(): Boolean = isOnlineInputBlocked()

    /** Se bloquea la entrada salvo que la sala esté en curso y sea el turno local. */
    private fun isOnlineInputBlocked(): Boolean =
        onlineStatus != MatchStatus.IN_PROGRESS || gameState.turn.playerId != localPlayerId

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
}
