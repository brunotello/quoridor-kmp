package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.CompetitiveState
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameResult
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.OnlineGameViewModel
import com.btello.quoridor.presentation.game.OnlineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class GameViewModelCompetitiveTest {

    private val config = GameConfig(playerCount = 2)

    private fun TestScope.onlineScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler))

    private fun winningState(winner: PlayerId): GameState =
        QuoridorRules.startGame(config).copy(status = GameStatus.GAME_OVER, winner = winner)

    private fun firstTo3State(wins: List<Int>): CompetitiveState =
        CompetitiveState(config = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3), wins = wins)

    private fun hostViewModel(
        scope: CoroutineScope,
        repo: FakeOnlineGameRepository,
    ) = OnlineGameViewModel(
        setup = GameSetup(
            config,
            online = OnlineSession(repo.matches.keys.first(), PlayerSlot.HOST),
        ),
        autoRunAi = false,
        onlineRepository = repo,
        onlineScope = scope,
    )

    @Test
    fun `winning a game without closing the series shows the intermediate state`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = hostViewModel(scope, repo)
        advanceUntilIdle()

        repo.pushRemoteState(id, winningState(PlayerId(0)), version = 1, competitive = firstTo3State(listOf(1, 0)))
        advanceUntilIdle()

        assertTrue(vm.uiState.isGameOver)
        assertFalse(vm.uiState.isSeriesOver)
        assertEquals(listOf(1, 0), vm.uiState.competitive?.wins)
        assertEquals(SeriesFormat.FIRST_TO_3, vm.uiState.competitive?.format)
        scope.cancel()
    }

    @Test
    fun `the game winner continues to the next game of the series`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = hostViewModel(scope, repo)
        advanceUntilIdle()
        repo.pushRemoteState(id, winningState(PlayerId(0)), version = 1, competitive = firstTo3State(listOf(1, 0)))
        advanceUntilIdle()

        vm.onEvent(GameEvent.ContinueSeries)
        advanceUntilIdle()

        assertFalse(vm.uiState.isGameOver)
        assertEquals(1, repo.current(id).competitive.gameIndex)
        assertEquals(MatchStatus.IN_PROGRESS, repo.current(id).status)
        scope.cancel()
    }

    @Test
    fun `reaching the required wins ends the series`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = hostViewModel(scope, repo)
        advanceUntilIdle()

        repo.pushRemoteState(id, winningState(PlayerId(0)), version = 1, competitive = firstTo3State(listOf(3, 0)))
        advanceUntilIdle()

        assertTrue(vm.uiState.isGameOver)
        assertTrue(vm.uiState.isSeriesOver)
        assertEquals(GameResult.WON, vm.uiState.localResult)
        scope.cancel()
    }

    private fun TestScope.timedViewModel(
        repo: FakeOnlineGameRepository,
        timeSource: TestTimeSource,
        scope: CoroutineScope,
    ): OnlineGameViewModel {
        val id = repo.matches.keys.first()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
            clockTimeSource = timeSource,
        )
        runCurrent()
        // El temporizador no corre durante la cuenta atrás previa al inicio: la dejamos terminar.
        advanceTimeBy(6_000)
        runCurrent()
        return vm
    }

    private suspend fun timedMatch(repo: FakeOnlineGameRepository, turnSeconds: Int) {
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(turnTimeSeconds = turnSeconds),
        )
        repo.simulateJoin(id, "Beto")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `running out of turn time passes the turn without losing the game`() = runTest {
        val repo = FakeOnlineGameRepository()
        timedMatch(repo, turnSeconds = 30)
        val timeSource = TestTimeSource()
        val scope = onlineScope()
        val vm = timedViewModel(repo, timeSource, scope)
        val id = repo.matches.keys.first()
        val before = repo.current(id)

        timeSource += 31.seconds
        advanceTimeBy(300)
        runCurrent()

        val after = repo.current(id)
        assertFalse(vm.uiState.isGameOver)
        assertEquals(PlayerId(1), after.state.turn.playerId)
        assertEquals(before.state.players, after.state.players)
        assertEquals(before.state.board, after.state.board)
        assertEquals(listOf(0, 0), after.competitive.wins)
        assertEquals(MatchStatus.IN_PROGRESS, after.status)
        scope.cancel()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `turn time does not expire before the limit`() = runTest {
        val repo = FakeOnlineGameRepository()
        timedMatch(repo, turnSeconds = 45)
        val timeSource = TestTimeSource()
        val scope = onlineScope()
        val vm = timedViewModel(repo, timeSource, scope)

        timeSource += 44.seconds
        advanceTimeBy(300)
        runCurrent()

        assertEquals(PlayerId(0), vm.uiState.gameState.turn.playerId)
        assertEquals(1_000L, vm.uiState.competitive?.turnRemainingMillis)
        scope.cancel()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `the turn timer restarts for the next turn`() = runTest {
        val repo = FakeOnlineGameRepository()
        timedMatch(repo, turnSeconds = 30)
        val timeSource = TestTimeSource()
        val scope = onlineScope()
        val vm = timedViewModel(repo, timeSource, scope)

        timeSource += 31.seconds
        advanceTimeBy(300)
        runCurrent()

        assertEquals(30_000L, vm.uiState.competitive?.turnRemainingMillis)
        scope.cancel()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `the rival's turn timeout is not applied locally`() = runTest {
        val repo = FakeOnlineGameRepository()
        timedMatch(repo, turnSeconds = 30)
        val timeSource = TestTimeSource()
        val scope = onlineScope()
        val vm = timedViewModel(repo, timeSource, scope)
        val id = repo.matches.keys.first()
        val rivalTurn = QuoridorRules.skipTurn(repo.current(id).state)
        repo.pushRemoteState(id, rivalTurn, version = 1)
        runCurrent()

        timeSource += 40.seconds
        advanceTimeBy(300)
        runCurrent()

        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
        assertEquals(1L, repo.current(id).version)
        assertEquals(0L, vm.uiState.competitive?.turnRemainingMillis)
        scope.cancel()
    }

    @Test
    fun `without turn timer the competitive ui has no countdown`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Beto")
        val scope = onlineScope()
        val vm = hostViewModel(scope, repo)
        advanceUntilIdle()

        assertNull(vm.uiState.competitive?.turnRemainingMillis)
        scope.cancel()
    }
}
