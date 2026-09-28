package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import com.btello.quoridor.presentation.game.OnlineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GameViewModelLeaveTest {

    private val config = GameConfig(playerCount = 2)

    private fun TestScope.onlineScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler))

    @Test
    fun `leaving a local game does not end it as a loss`() {
        val vm = GameViewModel(setup = GameSetup(config), autoRunAi = false)

        vm.onEvent(GameEvent.LeaveMatch)

        val state = vm.uiState
        assertFalse(state.isGameOver)
        assertEquals(GameStatus.IN_PROGRESS, state.gameState.status)
    }

    @Test
    fun `leaving an in-progress 1v1 online match ends it in favour of the rival`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host", AppConfig.VERSION)
        repo.simulateJoin(id, "Rival")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        vm.onEvent(GameEvent.LeaveMatch)
        advanceUntilIdle()

        val remote = repo.current(id)
        assertEquals(MatchStatus.FINISHED, remote.status)
        assertEquals(PlayerId(1), remote.state.winner)
        assertEquals(1, remote.state.players.size)
        scope.cancel()
    }

    @Test
    fun `leaving a 4-player online match removes the host and the rest keep playing`() = runTest {
        val fourPlayers = GameConfig(playerCount = 4)
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(fourPlayers, "Host", AppConfig.VERSION)
        repo.simulateJoin(id, "Rival 1")
        repo.simulateJoin(id, "Rival 2")
        repo.simulateJoin(id, "Rival 3")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(fourPlayers, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        vm.onEvent(GameEvent.LeaveMatch)
        advanceUntilIdle()

        val remote = repo.current(id)
        assertEquals(MatchStatus.IN_PROGRESS, remote.status)
        assertEquals(3, remote.state.players.size)
        assertFalse(remote.state.players.any { it.id == PlayerId(0) })
        assertEquals(GameStatus.IN_PROGRESS, remote.state.status)
        scope.cancel()
    }

    @Test
    fun `ending an online match clears the session for a new game`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host", AppConfig.VERSION)
        repo.simulateJoin(id, "Rival")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        vm.onEvent(GameEvent.NewGame)
        advanceUntilIdle()

        assertEquals(MatchStatus.ABANDONED, repo.current(id).status)
        scope.cancel()
    }
}
