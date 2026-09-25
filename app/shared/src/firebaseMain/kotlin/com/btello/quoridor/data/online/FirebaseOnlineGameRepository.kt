package com.btello.quoridor.data.online

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.OnlineMatch
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.rules.QuoridorRules
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/**
 * Implementación de [OnlineGameRepository] respaldada por Firebase Realtime
 * Database a través del SDK multiplataforma de GitLive.
 *
 * Cada partida es un único nodo `matches/{matchId}` con el [OnlineMatch]
 * serializado. Como Quoridor es por turnos, se sincroniza el [GameState] completo
 * y un contador [OnlineMatch.version] monotónico para ordenar las jugadas.
 *
 * Requiere que Firebase esté inicializado en el arranque de cada plataforma.
 */
internal class FirebaseOnlineGameRepository(
    private val database: FirebaseDatabase = Firebase.database,
) : OnlineGameRepository {

    override suspend fun createMatch(config: GameConfig, hostName: String): MatchId {
        val id = MatchId(generateCode())
        val ref = matchRef(id)
        val match = OnlineMatch(
            id = id,
            config = config,
            status = MatchStatus.WAITING,
            state = QuoridorRules.startGame(config),
            version = 0,
            playerNames = listOf(hostName),
            presence = listOf(true),
        )
        ref.setValue(OnlineMatch.serializer(), match) { encodeDefaults = true }
        ref.child(FIELD_PRESENCE).child(PlayerSlot.HOST.index.toString()).onDisconnect().setValue(false)
        return id
    }

    override suspend fun joinMatch(id: MatchId, playerName: String): Result<PlayerSlot> = runCatching {
        val ref = matchRef(id)
        val current = readMatch(ref) ?: throw NoSuchElementException("Match $id not found")
        check(current.status == MatchStatus.WAITING) { "Match $id is not joinable" }
        check(!current.isFull) { "Match $id is full" }
        val slot = PlayerSlot(current.joinedCount)
        val names = current.playerNames + playerName
        val presence = current.presence + true
        val willBeFull = names.size >= current.config.playerCount
        val updated = current.copy(
            status = if (willBeFull) MatchStatus.IN_PROGRESS else MatchStatus.WAITING,
            playerNames = names,
            presence = presence,
        )
        ref.setValue(OnlineMatch.serializer(), updated) { encodeDefaults = true }
        ref.child(FIELD_PRESENCE).child(slot.index.toString()).onDisconnect().setValue(false)
        slot
    }

    override fun observeMatch(id: MatchId): Flow<OnlineMatch> =
        matchRef(id).valueEvents
            .filter { it.exists }
            .map { it.value(OnlineMatch.serializer()) }

    override suspend fun submitMove(
        id: MatchId,
        newState: GameState,
        expectedVersion: Long,
    ): Result<Unit> = runCatching {
        val ref = matchRef(id)
        val current = readMatch(ref) ?: throw NoSuchElementException("Match $id not found")
        check(expectedVersion > current.version) { "Stale move for match $id" }
        val status = if (QuoridorRules.isGameOver(newState)) MatchStatus.FINISHED else current.status
        val updated = current.copy(state = newState, version = expectedVersion, status = status)
        ref.setValue(OnlineMatch.serializer(), updated) { encodeDefaults = true }
    }

    override suspend fun leaveMatch(id: MatchId, slot: PlayerSlot) {
        val ref = matchRef(id)
        runCatching {
            ref.child(FIELD_PRESENCE).child(slot.index.toString()).setValue(false)
            val current = readMatch(ref)
            if (current != null && current.status == MatchStatus.IN_PROGRESS) {
                ref.child(FIELD_STATUS).setValue(MatchStatus.ABANDONED)
            }
        }
    }

    private suspend fun readMatch(ref: DatabaseReference): OnlineMatch? {
        val snapshot = ref.valueEvents.first()
        return if (snapshot.exists) snapshot.value(OnlineMatch.serializer()) else null
    }

    private fun matchRef(id: MatchId): DatabaseReference =
        database.reference("$MATCHES_PATH/${id.value}")

    private fun generateCode(): String =
        (1..CODE_LENGTH).map { CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    private companion object {
        const val MATCHES_PATH = "matches"
        const val FIELD_PRESENCE = "presence"
        const val FIELD_STATUS = "status"
        const val CODE_LENGTH = 6
        const val CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
