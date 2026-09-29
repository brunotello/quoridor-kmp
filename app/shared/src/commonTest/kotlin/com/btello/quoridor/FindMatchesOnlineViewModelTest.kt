package com.btello.quoridor

import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.find.FindMatchesOnlineEvent
import com.btello.quoridor.presentation.online.find.FindMatchesOnlinePhase
import com.btello.quoridor.presentation.online.find.FindMatchesOnlineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FindMatchesOnlineViewModelTest {

    private fun viewModel(
        repo: FakeOnlineGameRepository,
        scope: CoroutineScope,
        name: String = "Ana",
    ) = FindMatchesOnlineViewModel(repo, scope, FakePlayerNameRepository(name))

    @Test
    fun `browsing lists the available public matches`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto", playerCount = 2)
        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro", playerCount = 4)
        val vm = viewModel(repo, this)

        advanceUntilIdle()

        val codes = vm.uiState.openMatches.map { it.id.value }
        assertEquals(listOf("PUB001", "PUB002"), codes)
        assertEquals("Beto", vm.uiState.openMatches.first().hostName)
        coroutineContext.cancelChildren()
    }

    @Test
    fun `the open list updates as matches fill up`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", playerCount = 2)
        val vm = viewModel(repo, this)

        advanceUntilIdle()
        assertEquals(1, vm.uiState.openMatches.size)

        repo.simulateJoin(id)
        advanceUntilIdle()

        assertTrue(vm.uiState.openMatches.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun `joining a public match starts on the assigned slot without a code`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", playerCount = 2)
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        advanceUntilIdle()
        vm.onEvent(FindMatchesOnlineEvent.JoinPublicMatch(id))
        advanceUntilIdle()

        val start = assertIs<OnlineSideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.GUEST, start.setup.online?.slot)
        assertEquals(id, start.setup.online?.matchId)
        collector.cancel()
        coroutineContext.cancelChildren()
    }

    @Test
    fun `refreshing the browse list restarts the observation`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto")
        val vm = viewModel(repo, this)

        advanceUntilIdle()
        assertEquals(1, vm.uiState.openMatches.size)

        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro")
        vm.onEvent(FindMatchesOnlineEvent.Refresh)
        advanceUntilIdle()

        assertEquals(listOf("PUB001", "PUB002"), vm.uiState.openMatches.map { it.id.value })
        coroutineContext.cancelChildren()
    }

    @Test
    fun `browsing hides public matches created with a different app version`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto", appVersion = AppConfig.VERSION)
        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro", appVersion = "0.0.1")
        val vm = viewModel(repo, this)

        advanceUntilIdle()

        assertEquals(listOf("PUB001"), vm.uiState.openMatches.map { it.id.value })
        coroutineContext.cancelChildren()
    }

    @Test
    fun `missing repository reports unsupported`() = runTest {
        val vm = FindMatchesOnlineViewModel(
            repository = null,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
        )
        advanceUntilIdle()
        assertEquals(OnlineError.Unsupported, vm.uiState.error)
    }

    @Test
    fun `joining a public match times out and surfaces a timeout error`() = runTest {
        val repo = HangingJoinOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.delegate.seedPublicMatch(id, hostName = "Beto")
        val vm = FindMatchesOnlineViewModel(
            repository = repo,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
            joinTimeoutMillis = 30_000L,
        )
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        advanceUntilIdle()
        vm.onEvent(FindMatchesOnlineEvent.JoinPublicMatch(id))

        advanceTimeBy(29_000L)
        assertEquals(FindMatchesOnlinePhase.Joining, vm.uiState.phase)

        advanceUntilIdle()

        assertEquals(OnlineError.JoinTimeout, vm.uiState.error)
        assertEquals(FindMatchesOnlinePhase.Idle, vm.uiState.phase)
        assertTrue(effects.isEmpty())
        collector.cancel()
        coroutineContext.cancelChildren()
    }
}
