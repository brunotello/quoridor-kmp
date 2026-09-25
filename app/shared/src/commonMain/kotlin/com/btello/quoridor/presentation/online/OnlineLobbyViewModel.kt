package com.btello.quoridor.presentation.online

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.data.player.PlayerNameProvider
import com.btello.quoridor.data.player.PlayerNameRepository
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.OnlineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del lobby online (feature `online`).
 *
 * Orquesta la creación de una sala (anfitrión que espera a los rivales) y la
 * unión a una sala existente por código, delegando en [OnlineGameRepository]. El
 * nombre del jugador se carga y persiste con [PlayerNameRepository] para poder
 * mostrarlo en el banner de la partida. Al quedar la sala en
 * [MatchStatus.IN_PROGRESS] emite [OnlineLobbySideEffect.StartGame] con el
 * [GameSetup] online correspondiente.
 *
 * El [scope] es inyectable para tests deterministas; en producción usa
 * `viewModelScope`.
 */
internal class OnlineLobbyViewModel(
    private val repository: OnlineGameRepository? = OnlinePlatform.repositoryOrNull(),
    scope: CoroutineScope? = null,
    private val playerNameRepository: PlayerNameRepository = PlayerNameProvider.repository,
) : ViewModel() {

    private val scope: CoroutineScope = scope ?: viewModelScope

    var uiState by mutableStateOf(
        playerNameRepository.name().let { name ->
            OnlineLobbyUiState(playerName = name, step = initialStep(name))
        },
    )
        private set

    private val _sideEffects = Channel<OnlineLobbySideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<OnlineLobbySideEffect> = _sideEffects.receiveAsFlow()

    private var hostedMatch: MatchId? = null
    private var waitJob: Job? = null

    fun onEvent(event: OnlineLobbyEvent) {
        when (event) {
            is OnlineLobbyEvent.NameChanged -> onNameChanged(event.name)
            OnlineLobbyEvent.ConfirmName -> confirmName()
            OnlineLobbyEvent.ChooseCreate -> uiState = uiState.copy(step = OnlineLobbyStep.Create, error = null)
            OnlineLobbyEvent.ChooseJoin -> uiState = uiState.copy(step = OnlineLobbyStep.Join, error = null)
            is OnlineLobbyEvent.PlayerCountChanged -> onPlayerCountChanged(event.count)
            OnlineLobbyEvent.CreateMatch -> createMatch()
            is OnlineLobbyEvent.JoinCodeChanged -> onJoinCodeChanged(event.code)
            OnlineLobbyEvent.JoinMatch -> joinMatch()
            OnlineLobbyEvent.NavigateBack -> navigateBack()
            OnlineLobbyEvent.Cancel -> cancel()
        }
    }

    private fun onNameChanged(name: String) {
        uiState = uiState.copy(playerName = name, error = null)
    }

    /** Persiste el nombre ingresado y avanza al menú de acciones. */
    private fun confirmName() {
        if (!uiState.canConfirmName) return
        val name = persistName()
        uiState = uiState.copy(playerName = name, step = OnlineLobbyStep.Menu, error = null)
    }

    /** Vuelve del paso de crear/unirse al menú, cancelando la espera si estaba hospedando. */
    private fun navigateBack() {
        when (uiState.step) {
            OnlineLobbyStep.Create -> {
                resetHosting()
                uiState = uiState.copy(
                    step = OnlineLobbyStep.Menu,
                    phase = OnlineLobbyPhase.Idle,
                    hostedCode = null,
                    error = null,
                )
            }

            OnlineLobbyStep.Join -> uiState = uiState.copy(step = OnlineLobbyStep.Menu, error = null)
            OnlineLobbyStep.Name, OnlineLobbyStep.Menu -> Unit
        }
    }

    private fun onPlayerCountChanged(count: Int) {
        uiState = uiState.copy(playerCount = count, error = null)
    }

    private fun onJoinCodeChanged(code: String) {
        uiState = uiState.copy(joinCode = code.trim().uppercase(), error = null)
    }

    private fun createMatch() {
        val repo = repository ?: run {
            uiState = uiState.copy(error = OnlineLobbyError.Unsupported)
            return
        }
        if (!uiState.canCreate) return
        val name = persistName()
        val playerCount = uiState.playerCount
        uiState = uiState.copy(phase = OnlineLobbyPhase.Creating, error = null)
        scope.launch {
            val id = repo.createMatch(GameConfig(playerCount = playerCount), name)
            hostedMatch = id
            uiState = uiState.copy(
                phase = OnlineLobbyPhase.WaitingForOpponent,
                hostedCode = id.value,
                joinedCount = 1,
            )
            awaitOpponents(repo, id)
        }
    }

    private fun awaitOpponents(repo: OnlineGameRepository, id: MatchId) {
        waitJob = scope.launch {
            repo.observeMatch(id).first { match ->
                uiState = uiState.copy(joinedCount = match.joinedCount)
                match.status == MatchStatus.IN_PROGRESS
            }
            emitStart(id, PlayerSlot.HOST, uiState.playerCount)
        }
    }

    private fun joinMatch() {
        val repo = repository ?: run {
            uiState = uiState.copy(error = OnlineLobbyError.Unsupported)
            return
        }
        if (!uiState.canJoin) return
        val name = persistName()
        val id = MatchId(uiState.joinCode)
        uiState = uiState.copy(phase = OnlineLobbyPhase.Joining, error = null)
        scope.launch {
            repo.joinMatch(id, name).fold(
                onSuccess = { slot ->
                    val playerCount = repo.observeMatch(id).first().config.playerCount
                    emitStart(id, slot, playerCount)
                },
                onFailure = { throwable ->
                    uiState = uiState.copy(phase = OnlineLobbyPhase.Idle, error = throwable.toLobbyError())
                },
            )
        }
    }

    /** Guarda el nombre actual (recortado) en preferencias y lo devuelve. */
    private fun persistName(): String {
        val name = uiState.playerName.trim()
        playerNameRepository.setName(name)
        uiState = uiState.copy(playerName = name)
        return name
    }

    private fun emitStart(id: MatchId, slot: PlayerSlot, playerCount: Int) {
        _sideEffects.trySend(
            OnlineLobbySideEffect.StartGame(
                GameSetup(
                    config = GameConfig(playerCount = playerCount),
                    online = OnlineSession(matchId = id, slot = slot),
                ),
            ),
        )
    }

    private fun cancel() {
        resetHosting()
        uiState = uiState.copy(
            step = OnlineLobbyStep.Create,
            phase = OnlineLobbyPhase.Idle,
            hostedCode = null,
            joinedCount = 1,
            error = null,
        )
    }

    /** Cancela la espera de rivales y abandona la sala hospedada, si la hubiera. */
    private fun resetHosting() {
        waitJob?.cancel()
        waitJob = null
        val repo = repository
        val hosted = hostedMatch
        if (repo != null && hosted != null) {
            scope.launch { repo.leaveMatch(hosted, PlayerSlot.HOST) }
        }
        hostedMatch = null
    }
}

/** Paso inicial: pedir el nombre si aún no está definido, o ir directo al menú. */
private fun initialStep(name: String): OnlineLobbyStep =
    if (name.isBlank()) OnlineLobbyStep.Name else OnlineLobbyStep.Menu

/** Traduce la excepción de una unión fallida a un [OnlineLobbyError] mostrable. */
private fun Throwable.toLobbyError(): OnlineLobbyError = when (this) {
    is NoSuchElementException -> OnlineLobbyError.NotFound
    is IllegalStateException -> OnlineLobbyError.NotJoinable
    else -> OnlineLobbyError.Connection
}
