package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.GameResult
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
import kotlin.test.assertNull

class GameViewModelResultTest {

    private val config = GameConfig(playerCount = 2)

    private fun TestScope.onlineScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler))

    /** Estado terminal con [winner] ya declarado, para inyectar por la sala online. */
    private fun winningState(winner: PlayerId): GameState =
        QuoridorRules.startGame(config).copy(status = GameStatus.GAME_OVER, winner = winner)

    private fun onlineViewModel(
        scope: CoroutineScope,
        repo: FakeOnlineGameRepository,
        slot: PlayerSlot,
    ) = GameViewModel(
        setup = GameSetup(config, online = OnlineSession(repo.matches.keys.first(), slot)),
        autoRunAi = false,
        onlineRepository = repo,
        onlineScope = scope,
    )

    @Test
    fun `host that reaches the goal sees a win`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana", AppConfig.VERSION)
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = onlineViewModel(scope, repo, PlayerSlot.HOST)
        advanceUntilIdle()

        repo.pushRemoteState(id, winningState(PlayerId(0)), version = 1)
        advanceUntilIdle()

        assertEquals(GameResult.WON, vm.uiState.localResult)
        scope.cancel()
    }

    @Test
    fun `guest sees a loss when the host reaches the goal`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana", AppConfig.VERSION)
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = onlineViewModel(scope, repo, PlayerSlot.GUEST)
        advanceUntilIdle()

        repo.pushRemoteState(id, winningState(PlayerId(0)), version = 1)
        advanceUntilIdle()

        assertEquals(GameResult.LOST, vm.uiState.localResult)
        scope.cancel()
    }

    @Test
    fun `an opponent leaving counts as a local win`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana", AppConfig.VERSION)
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = onlineViewModel(scope, repo, PlayerSlot.HOST)
        advanceUntilIdle()

        repo.leaveMatch(id, PlayerSlot.GUEST)
        advanceUntilIdle()

        assertEquals(GameResult.WON, vm.uiState.localResult)
        scope.cancel()
    }

    @Test
    fun `a shared local game has no local win or loss perspective`() {
        val vm = GameViewModel(
            setup = GameSetup(config = GameConfig(playerCount = 2)),
            autoRunAi = false,
        )
        assertNull(vm.uiState.localResult)
    }

    @Test
    fun `versus ai the single human is the local perspective`() {
        val vm = GameViewModel(
            setup = GameSetup(
                config = GameConfig(playerCount = 2),
                aiPlayers = setOf(PlayerId(1)),
            ),
            autoRunAi = false,
        )
        // Sin ganador todavía, pero la perspectiva local existe (humano = jugador 0).
        assertNull(vm.uiState.localResult)
        assertNull(vm.uiState.winnerNumber)
    }
}
