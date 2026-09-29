package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.online.IncompatibleVersionException
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.OnlineMatch
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositorio online en memoria para tests deterministas (sin Firebase). Cada
 * sala es un [MutableStateFlow] que los tests pueden manipular con [simulateJoin]
 * y [pushRemoteState] para emular a los rivales. [revision] se incrementa en cada
 * mutación para que [observeOpenMatches] recomponga la lista de salas públicas.
 */
internal class FakeOnlineGameRepository(
    private val fixedId: String = "ROOM01",
) : OnlineGameRepository {

    val matches = mutableMapOf<MatchId, MutableStateFlow<OnlineMatch>>()

    private val revision = MutableStateFlow(0)

    override suspend fun createMatch(
        config: GameConfig,
        hostName: String,
        appVersion: String,
        isPublic: Boolean,
    ): MatchId {
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
                isPublic = isPublic,
                appVersion = appVersion,
            ),
        )
        bumpRevision()
        return id
    }

    override fun observeOpenMatches(appVersion: String): Flow<List<OnlineMatch>> = revision.map {
        matches.values
            .map { it.value }
            .filter { it.isOpenToPublic && it.isCompatibleWith(appVersion) }
            .sortedBy { it.id.value }
    }

    override suspend fun joinMatch(
        id: MatchId,
        playerName: String,
        appVersion: String,
    ): Result<PlayerSlot> {
        val flow = matches[id] ?: return Result.failure(NoSuchElementException("not found"))
        val current = flow.value
        if (current.status != MatchStatus.WAITING) {
            return Result.failure(IllegalStateException("not joinable"))
        }
        if (current.isFull) {
            return Result.failure(IllegalStateException("full"))
        }
        if (!current.isCompatibleWith(appVersion)) {
            return Result.failure(
                IncompatibleVersionException(
                    requiredVersion = current.appVersion,
                    localVersion = appVersion,
                ),
            )
        }
        val slot = PlayerSlot(current.joinedCount)
        flow.value = appendPlayer(current, playerName)
        bumpRevision()
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
        bumpRevision()
        return Result.success(Unit)
    }

    override suspend fun leaveMatch(id: MatchId, slot: PlayerSlot) {
        val flow = matches[id] ?: return
        flow.value = flow.value.copy(status = MatchStatus.ABANDONED)
        bumpRevision()
    }

    /** Emula a un rival uniéndose a la sala [id] (asigna el siguiente asiento). */
    fun simulateJoin(id: MatchId, name: String = "Rival") {
        val flow = matches.getValue(id)
        flow.value = appendPlayer(flow.value, name)
        bumpRevision()
    }

    /** Siembra una sala pública ya existente (para tests del lobby público). */
    fun seedPublicMatch(
        id: MatchId,
        hostName: String,
        playerCount: Int = 2,
        appVersion: String = AppConfig.VERSION,
    ) {
        val config = GameConfig(playerCount = playerCount)
        matches[id] = MutableStateFlow(
            OnlineMatch(
                id = id,
                config = config,
                status = MatchStatus.WAITING,
                state = QuoridorRules.startGame(config),
                version = 0,
                playerNames = listOf(hostName),
                presence = listOf(true),
                isPublic = true,
                appVersion = appVersion,
            ),
        )
        bumpRevision()
    }

    fun pushRemoteState(id: MatchId, state: GameState, version: Long) {
        val flow = matches.getValue(id)
        flow.value = flow.value.copy(state = state, version = version)
        bumpRevision()
    }

    fun current(id: MatchId): OnlineMatch = matches.getValue(id).value

    private fun bumpRevision() {
        revision.value += 1
    }

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
