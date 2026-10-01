package com.btello.quoridor.presentation.online.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btello.quoridor.AppConfig
import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.data.player.PlayerNameProvider
import com.btello.quoridor.data.player.PlayerNameRepository
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.OnlineSession
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineSideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de creación de salas online (feature `online`).
 *
 * Crea una sala como anfitrión, espera a que se unan los rivales delegando en
 * [OnlineGameRepository], y al quedar la sala [MatchStatus.IN_PROGRESS] emite
 * [OnlineSideEffect.StartGame]. El nombre del jugador se carga desde
 * [PlayerNameRepository] (ya persistido en la pantalla principal).
 *
 * El [scope] es inyectable para tests deterministas; en producción usa
 * `viewModelScope`.
 */
internal class CreateMatchOnlineViewModel(
    private val repository: OnlineGameRepository? = OnlinePlatform.repositoryOrNull(),
    scope: CoroutineScope? = null,
    playerNameRepository: PlayerNameRepository = PlayerNameProvider.repository,
    private val appVersion: String = AppConfig.VERSION,
) : ViewModel() {

    private val scope: CoroutineScope = scope ?: viewModelScope

    var uiState by mutableStateOf(
        CreateMatchOnlineUiState(playerName = playerNameRepository.name()),
    )
        private set

    private val _sideEffects = Channel<OnlineSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<OnlineSideEffect> = _sideEffects.receiveAsFlow()

    private var hostedMatch: MatchId? = null
    private var waitJob: Job? = null

    fun onEvent(event: CreateMatchOnlineEvent) {
        when (event) {
            is CreateMatchOnlineEvent.PlayerCountChanged ->
                uiState = uiState.copy(playerCount = event.count, error = null)

            is CreateMatchOnlineEvent.VisibilityChanged ->
                uiState = uiState.copy(isPublic = event.isPublic, error = null)

            is CreateMatchOnlineEvent.FormatChanged ->
                uiState = uiState.copy(format = event.format, error = null)

            is CreateMatchOnlineEvent.TurnTimeChanged ->
                uiState = uiState.copy(turnTimeSeconds = event.seconds, error = null)

            CreateMatchOnlineEvent.CreateMatch -> createMatch()
            CreateMatchOnlineEvent.Cancel -> cancel()
        }
    }

    private fun createMatch() {
        val repo = repository ?: run {
            uiState = uiState.copy(error = OnlineError.Unsupported)
            return
        }
        if (!uiState.canCreate) return
        val name = uiState.playerName.trim()
        val playerCount = uiState.playerCount
        val isPublic = uiState.isPublic
        val competitive = CompetitiveConfig(
            format = uiState.format,
            turnTimeSeconds = uiState.turnTimeSeconds,
        )
        uiState = uiState.copy(phase = CreateMatchOnlinePhase.Creating, error = null)
        scope.launch {
            val id = repo.createMatch(
                GameConfig(playerCount = playerCount),
                name,
                appVersion,
                isPublic,
                competitive,
            )
            hostedMatch = id
            uiState = uiState.copy(
                phase = CreateMatchOnlinePhase.WaitingForOpponent,
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
            emitStart(id, uiState.playerCount)
        }
    }

    private fun emitStart(id: MatchId, playerCount: Int) {
        _sideEffects.trySend(
            OnlineSideEffect.StartGame(
                GameSetup(
                    config = GameConfig(playerCount = playerCount),
                    online = OnlineSession(matchId = id, slot = PlayerSlot.HOST),
                ),
            ),
        )
    }

    private fun cancel() {
        resetHosting()
        uiState = uiState.copy(
            phase = CreateMatchOnlinePhase.Idle,
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

    override fun onCleared() {
        resetHosting()
    }
}
