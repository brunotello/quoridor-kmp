package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameFeedback
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import com.btello.quoridor.presentation.game.OnlineSession
import com.btello.quoridor.presentation.game.TurnBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameViewModelOnlineTest {

    private val config = GameConfig(playerCount = 2)

    private fun firstPawnMove(playerId: PlayerId) =
        QuoridorRules.getLegalMoves(QuoridorRules.startGame(config))
            .filterIsInstance<Move.PawnMove>()
            .first { it.playerId == playerId }

    /**
     * Scope de observación online independiente del cuerpo del test: se ejecuta
     * bajo el mismo [TestScope.testScheduler] (por lo que `advanceUntilIdle` lo
     * conduce) pero no es hijo de la corrutina del test, de modo que el collector
     * infinito de la sala no bloquea el cierre de `runTest`.
     */
    private fun TestScope.onlineScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler))

    @Test
    fun `host waiting for opponent surfaces waiting feedback and blocks input`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        assertEquals(GameFeedback.WaitingOpponent, vm.uiState.feedback)
        assertTrue(vm.uiState.legalTargets.isEmpty())
        scope.cancel()
    }

    @Test
    fun `guest input is blocked and turn banner shows the host name`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana")
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.GUEST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        val banner = assertIs<TurnBanner.PlayerTurn>(vm.uiState.turnBanner)
        assertEquals("Ana", banner.playerName)
        assertEquals(listOf("Ana", "Beto"), vm.uiState.playerNames)
        assertTrue(vm.uiState.legalTargets.isEmpty())
        scope.cancel()
    }

    @Test
    fun `guest adopts a remote host move and then can play`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana")
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.GUEST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        val afterHost = QuoridorRules.applyMove(
            QuoridorRules.startGame(config),
            firstPawnMove(PlayerId(0)),
        ).state!!
        repo.pushRemoteState(id, afterHost, version = 1)
        advanceUntilIdle()

        assertEquals(afterHost, vm.uiState.gameState)
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
        assertEquals(TurnBanner.YourTurn, vm.uiState.turnBanner)
        assertTrue(vm.uiState.legalTargets.isNotEmpty())

        vm.onEvent(GameEvent.CellClick(vm.uiState.legalTargets.first()))
        advanceUntilIdle()

        assertEquals(2, repo.current(id).version)
        assertEquals(PlayerId(0), repo.current(id).state.turn.playerId)
        scope.cancel()
    }

    @Test
    fun `host move is published with an incremented version`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana")
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.legalTargets.isNotEmpty())
        vm.onEvent(GameEvent.CellClick(vm.uiState.legalTargets.first()))
        advanceUntilIdle()

        assertEquals(1, repo.current(id).version)
        scope.cancel()
    }

    @Test
    fun `opponent leaving ends the game for everyone`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana")
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = GameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        repo.leaveMatch(id, PlayerSlot.GUEST)
        advanceUntilIdle()

        assertTrue(vm.uiState.isGameOver)
        assertTrue(vm.uiState.isAbandoned)
        assertTrue(vm.uiState.legalTargets.isEmpty())
        scope.cancel()
    }

    @Test
    fun `leaving the match on new game marks it abandoned for the rival`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Ana")
        repo.simulateJoin(id, "Beto")
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
