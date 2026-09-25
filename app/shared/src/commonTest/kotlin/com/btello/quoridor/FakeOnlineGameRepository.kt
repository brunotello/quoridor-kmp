package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.OnlineMatch
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio online en memoria para tests deterministas (sin Firebase). Cada
 * sala es un [MutableStateFlow] que los tests pueden manipular con [simulateJoin]
 * y [pushRemoteState] para emular a los rivales.
 */
internal class FakeOnlineGameRepository(
    private val fixedId: String = "ROOM01",
) : OnlineGameRepository {

    val matches = mutableMapOf<MatchId, MutableStateFlow<OnlineMatch>>()

    override suspend fun createMatch(config: GameConfig, hostName: String): MatchId {
        val id = MatchId(fixedId)
        matches[id] = MutableStateFlow(
            OnlineMatch(
                id = id,
                config = config,
                status = MatchStatus.WAITING,
                state = QuoridorRules.startGame(config),
                version = 0,
                playerNames = listOf(hostName),
                presence = listOf(true),
            ),
        )
        return id
    }

    override suspend fun joinMatch(id: MatchId, playerName: String): Result<PlayerSlot> {
        val flow = matches[id] ?: return Result.failure(NoSuchElementException("not found"))
        val current = flow.value
        if (current.status != MatchStatus.WAITING) {
            return Result.failure(IllegalStateException("not joinable"))
        }
        if (current.isFull) {
            return Result.failure(IllegalStateException("full"))
        }
        val slot = PlayerSlot(current.joinedCount)
        flow.value = appendPlayer(current, playerName)
        return Result.success(slot)
    }

    override fun observeMatch(id: MatchId): Flow<OnlineMatch> = matches.getValue(id)

    override suspend fun submitMove(
        id: MatchId,
        newState: GameState,
        expectedVersion: Long,
    ): Result<Unit> {
        val flow = matches[id] ?: return Result.failure(NoSuchElementException("not found"))
        if (expectedVersion <= flow.value.version) {
            return Result.failure(IllegalStateException("stale"))
        }
        val status = if (QuoridorRules.isGameOver(newState)) MatchStatus.FINISHED else flow.value.status
        flow.value = flow.value.copy(state = newState, version = expectedVersion, status = status)
        return Result.success(Unit)
    }

    override suspend fun leaveMatch(id: MatchId, slot: PlayerSlot) {
        val flow = matches[id] ?: return
        flow.value = flow.value.copy(status = MatchStatus.ABANDONED)
    }

    /** Emula a un rival uniéndose a la sala [id] (asigna el siguiente asiento). */
    fun simulateJoin(id: MatchId, name: String = "Rival") {
        val flow = matches.getValue(id)
        flow.value = appendPlayer(flow.value, name)
    }

    fun pushRemoteState(id: MatchId, state: GameState, version: Long) {
        val flow = matches.getValue(id)
        flow.value = flow.value.copy(state = state, version = version)
    }

    fun current(id: MatchId): OnlineMatch = matches.getValue(id).value

    private fun appendPlayer(match: OnlineMatch, name: String): OnlineMatch {
        val names = match.playerNames + name
        val presence = match.presence + true
        val willBeFull = names.size >= match.config.playerCount
        return match.copy(
            status = if (willBeFull) MatchStatus.IN_PROGRESS else MatchStatus.WAITING,
            playerNames = names,
            presence = presence,
        )
    }
}
