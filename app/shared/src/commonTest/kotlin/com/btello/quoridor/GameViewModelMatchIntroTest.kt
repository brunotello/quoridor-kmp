package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.MatchIntro
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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameViewModelMatchIntroTest {

    private val config = GameConfig(playerCount = 2)

    private fun TestScope.onlineScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler))

    @Test
    fun `host waiting for players shows the waiting intro over the board`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host", AppConfig.VERSION)
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        assertEquals(MatchIntro.WaitingForPlayers, vm.uiState.matchIntro)
        assertTrue(vm.uiState.legalTargets.isEmpty())
        scope.cancel()
    }

    @Test
    fun `once every player joins a countdown runs and blocks the board until it ends`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host", AppConfig.VERSION)
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        repo.simulateJoin(id, "Rival")
        runCurrent()

        assertEquals(MatchIntro.Countdown(5), vm.uiState.matchIntro)
        assertTrue(vm.uiState.legalTargets.isEmpty())

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(MatchIntro.Countdown(4), vm.uiState.matchIntro)

        advanceUntilIdle()
        assertNull(vm.uiState.matchIntro)
        assertEquals(MatchStatus.IN_PROGRESS, repo.current(id).status)
        assertTrue(vm.uiState.legalTargets.isNotEmpty())
        scope.cancel()
    }

    @Test
    fun `the intro countdown ends with the game-start announcement`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(config, "Host", AppConfig.VERSION)
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.HOST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        repo.simulateJoin(id, "Rival")
        runCurrent()
        // Advance through 5, 4, 3, 2, 1 to reach the "0" that announces the start.
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(MatchIntro.Countdown(0), vm.uiState.matchIntro)
        scope.cancel()
    }

    @Test
    fun `a remaining player keeps playing after a rival leaves a 4-player match`() = runTest {
        val fourPlayers = GameConfig(playerCount = 4)
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(fourPlayers, "Host", AppConfig.VERSION)
        repo.simulateJoin(id, "Rival 1")
        repo.simulateJoin(id, "Rival 2")
        repo.simulateJoin(id, "Rival 3")
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(fourPlayers, online = OnlineSession(id, PlayerSlot.GUEST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        val hostLeft = QuoridorRules.withPlayerRemoved(
            QuoridorRules.startGame(fourPlayers),
            PlayerId(0),
        )
        repo.pushRemoteState(id, hostLeft, version = 1)
        advanceUntilIdle()

        val state = vm.uiState.gameState
        assertEquals(3, state.players.size)
        assertFalse(state.players.any { it.id == PlayerId(0) })
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertFalse(vm.uiState.isGameOver)
        assertEquals(PlayerId(1), state.turn.playerId)
        assertTrue(vm.uiState.legalTargets.isNotEmpty())
        scope.cancel()
    }

    @Test
    fun `a player who leaves a competitive match is dropped from the panel`() = runTest {
        val fourPlayers = GameConfig(playerCount = 4)
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            fourPlayers,
            "Host",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Rival 1")
        repo.simulateJoin(id, "Rival 2")
        repo.simulateJoin(id, "Rival 3")
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(fourPlayers, online = OnlineSession(id, PlayerSlot.GUEST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        assertEquals(setOf(0, 1, 2, 3), vm.uiState.competitive?.presentPlayerIds)

        val hostLeft = QuoridorRules.withPlayerRemoved(
            QuoridorRules.startGame(fourPlayers),
            PlayerId(0),
        )
        repo.pushRemoteState(
            id,
            hostLeft,
            version = 1,
            presence = listOf(false, true, true, true),
        )
        advanceUntilIdle()

        assertEquals(setOf(1, 2, 3), vm.uiState.competitive?.presentPlayerIds)
        scope.cancel()
    }

    @Test
    fun `a player who loses a game on time still shows in the panel`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(
            config,
            "Host",
            AppConfig.VERSION,
            competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3),
        )
        repo.simulateJoin(id, "Rival")
        val scope = onlineScope()
        val vm = OnlineGameViewModel(
            setup = GameSetup(config, online = OnlineSession(id, PlayerSlot.GUEST)),
            autoRunAi = false,
            onlineRepository = repo,
            onlineScope = scope,
        )
        advanceUntilIdle()

        assertEquals(setOf(0, 1), vm.uiState.competitive?.presentPlayerIds)

        // El host (asiento 0) pierde este juego por tiempo: se lo quita del tablero
        // pero sigue presente, porque continúa en la serie.
        val hostTimedOut = QuoridorRules.withPlayerRemoved(
            QuoridorRules.startGame(config),
            PlayerId(0),
        )
        repo.pushRemoteState(
            id,
            hostTimedOut,
            version = 1,
            presence = listOf(true, true),
        )
        advanceUntilIdle()

        assertEquals(setOf(0, 1), vm.uiState.competitive?.presentPlayerIds)
        scope.cancel()
    }

    @Test
    fun `the starting player is randomized so the host is not always first`() = runTest {
        val fourPlayers = GameConfig(playerCount = 4)
        val startedFirst = mutableSetOf<PlayerId>()
        repeat(20) { seed ->
            val repo = FakeOnlineGameRepository(startingPlayerRandom = Random(seed))
            val id = repo.createMatch(fourPlayers, "Host", AppConfig.VERSION)
            repo.simulateJoin(id, "Rival 1")
            repo.simulateJoin(id, "Rival 2")
            repo.simulateJoin(id, "Rival 3")
            val scope = onlineScope()
            val vm = OnlineGameViewModel(
                setup = GameSetup(fourPlayers, online = OnlineSession(id, PlayerSlot.HOST)),
                autoRunAi = false,
                onlineRepository = repo,
                onlineScope = scope,
            )
            advanceUntilIdle()
            startedFirst += vm.uiState.gameState.turn.playerId
            scope.cancel()
        }

        assertTrue(startedFirst.any { it != PlayerId(0) })
    }
}
