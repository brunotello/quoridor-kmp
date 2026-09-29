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

    @OptIn(ExperimentalTime::class)
    @Test
    fun `running out of time loses the game`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Ana",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(timeControlSeconds = 5),
        )
        repo.simulateJoin(id, "Beto")
        val timeSource = TestTimeSource()
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
            clockTimeSource = timeSource,
        )
        runCurrent()
        // El reloj no corre durante la cuenta atrás previa al inicio: la dejamos terminar.
        advanceTimeBy(6_000)
        runCurrent()

        timeSource += 6.seconds
        advanceTimeBy(300)
        runCurrent()

        assertTrue(vm.uiState.isGameOver)
        assertEquals(GameResult.LOST, vm.uiState.localResult)
        assertEquals(listOf(0, 1), repo.current(id).competitive.wins)
        scope.cancel()
    }
}
