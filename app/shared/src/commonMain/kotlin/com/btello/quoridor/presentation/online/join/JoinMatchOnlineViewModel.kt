package com.btello.quoridor.presentation.online.join

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
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.OnlineSession
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.ONLINE_JOIN_TIMEOUT_MILLIS
import com.btello.quoridor.presentation.online.toOnlineError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * ViewModel de la pantalla de unión por código (feature `online`).
 *
 * Se une a una sala existente delegando en [OnlineGameRepository] y, al lograrlo,
 * emite [OnlineSideEffect.StartGame]. La unión está acotada por
 * [joinTimeoutMillis] (30 s por defecto): si no se completa a tiempo se muestra
 * un error de tiempo agotado para que el usuario reintente.
 *
 * El [scope] es inyectable para tests deterministas; en producción usa
 * `viewModelScope`.
 */
internal class JoinMatchOnlineViewModel(
    private val repository: OnlineGameRepository? = OnlinePlatform.repositoryOrNull(),
    scope: CoroutineScope? = null,
    playerNameRepository: PlayerNameRepository = PlayerNameProvider.repository,
    private val appVersion: String = AppConfig.VERSION,
    private val joinTimeoutMillis: Long = ONLINE_JOIN_TIMEOUT_MILLIS,
) : ViewModel() {

    private val scope: CoroutineScope = scope ?: viewModelScope

    var uiState by mutableStateOf(
        JoinMatchOnlineUiState(playerName = playerNameRepository.name()),
    )
        private set

    private val _sideEffects = Channel<OnlineSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<OnlineSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: JoinMatchOnlineEvent) {
        when (event) {
            is JoinMatchOnlineEvent.JoinCodeChanged ->
                uiState = uiState.copy(joinCode = event.code.trim().uppercase(), error = null)

            JoinMatchOnlineEvent.JoinMatch -> joinByCode()
        }
    }

    private fun joinByCode() {
        val repo = repository ?: run {
            uiState = uiState.copy(error = OnlineError.Unsupported)
            return
        }
        if (!uiState.canJoinByCode) return
        val id = MatchId(uiState.joinCode.trim())
        val name = uiState.playerName.trim()
        uiState = uiState.copy(phase = JoinMatchOnlinePhase.Joining, error = null)
        scope.launch {
            try {
                withTimeout(joinTimeoutMillis) {
                    repo.joinMatch(id, name, appVersion).fold(
                        onSuccess = { slot ->
                            val playerCount = repo.observeMatch(id).first().config.playerCount
                            emitStart(id, slot, playerCount)
                        },
                        onFailure = { throwable ->
                            uiState = uiState.copy(
                                phase = JoinMatchOnlinePhase.Idle,
                                error = throwable.toOnlineError(),
                            )
                        },
                    )
                }
            } catch (_: TimeoutCancellationException) {
                uiState = uiState.copy(
                    phase = JoinMatchOnlinePhase.Idle,
                    error = OnlineError.JoinTimeout,
                )
            }
        }
    }

    private fun emitStart(id: MatchId, slot: PlayerSlot, playerCount: Int) {
        _sideEffects.trySend(
            OnlineSideEffect.StartGame(
                GameSetup(
                    config = GameConfig(playerCount = playerCount),
                    online = OnlineSession(matchId = id, slot = slot),
                ),
            ),
        )
    }
}
