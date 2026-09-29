package com.btello.quoridor

import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.OnlineGameRepository
import com.btello.quoridor.domain.online.PlayerSlot
import kotlinx.coroutines.awaitCancellation

/**
 * Repositorio online que nunca completa [joinMatch] (suspende hasta ser
 * cancelado), para probar el tiempo límite de unión de los ViewModels. Delega el
 * resto de operaciones en un [FakeOnlineGameRepository] real.
 */
internal class HangingJoinOnlineGameRepository(
    val delegate: FakeOnlineGameRepository = FakeOnlineGameRepository(),
) : OnlineGameRepository by delegate {

    override suspend fun joinMatch(
        id: MatchId,
        playerName: String,
        appVersion: String,
    ): Result<PlayerSlot> = awaitCancellation()
}
