package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.join.JoinMatchOnlineEvent
import com.btello.quoridor.presentation.online.join.JoinMatchOnlinePhase
import com.btello.quoridor.presentation.online.join.JoinMatchOnlineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JoinMatchOnlineViewModelTest {

    private fun viewModel(
        repo: FakeOnlineGameRepository,
        scope: CoroutineScope,
        name: String = "Ana",
    ) = JoinMatchOnlineViewModel(repo, scope, FakePlayerNameRepository(name))

    @Test
    fun `joining an existing match starts on the assigned slot`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(GameConfig(playerCount = 4), "Host", AppConfig.VERSION)
        repo.simulateJoin(id)
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged(id.value))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)
        advanceUntilIdle()

        val start = assertIs<OnlineSideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot(2), start.setup.online?.slot)
        assertEquals(id, start.setup.online?.matchId)
        assertEquals(4, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `join code is trimmed and uppercased`() = runTest {
        val vm = viewModel(FakeOnlineGameRepository(), this)
        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged("  room01 "))
        assertEquals("ROOM01", vm.uiState.joinCode)
    }

    @Test
    fun `joining an unknown code surfaces a not found error`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged("NOPE12"))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)
        advanceUntilIdle()

        assertEquals(OnlineError.NotFound, vm.uiState.error)
        assertEquals(JoinMatchOnlinePhase.Idle, vm.uiState.phase)
    }

    @Test
    fun `joining a match with an incompatible version surfaces a version error`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", appVersion = "0.0.1")
        val vm = viewModel(repo, this)

        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged(id.value))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)
        advanceUntilIdle()

        assertEquals(OnlineError.IncompatibleVersion, vm.uiState.error)
        assertEquals(JoinMatchOnlinePhase.Idle, vm.uiState.phase)
    }

    @Test
    fun `missing repository reports unsupported`() = runTest {
        val vm = JoinMatchOnlineViewModel(
            repository = null,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
        )
        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged("ROOM01"))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)
        advanceUntilIdle()
        assertEquals(OnlineError.Unsupported, vm.uiState.error)
    }

    @Test
    fun `joining times out and surfaces a timeout error`() = runTest {
        val repo = HangingJoinOnlineGameRepository()
        repo.delegate.seedPublicMatch(MatchId("ROOM01"), hostName = "Beto")
        val vm = JoinMatchOnlineViewModel(
            repository = repo,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
            joinTimeoutMillis = 30_000L,
        )

        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged("ROOM01"))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)

        advanceTimeBy(29_000L)
        assertEquals(JoinMatchOnlinePhase.Joining, vm.uiState.phase)

        advanceUntilIdle()

        assertEquals(OnlineError.JoinTimeout, vm.uiState.error)
        assertEquals(JoinMatchOnlinePhase.Idle, vm.uiState.phase)
    }

    @Test
    fun `no side effect is emitted when joining times out`() = runTest {
        val repo = HangingJoinOnlineGameRepository()
        repo.delegate.seedPublicMatch(MatchId("ROOM01"), hostName = "Beto")
        val vm = JoinMatchOnlineViewModel(
            repository = repo,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
        )
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(JoinMatchOnlineEvent.JoinCodeChanged("ROOM01"))
        vm.onEvent(JoinMatchOnlineEvent.JoinMatch)
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        assertEquals(OnlineError.JoinTimeout, vm.uiState.error)
        collector.cancel()
    }
}
