package com.btello.quoridor

import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.create.CreateMatchOnlineEvent
import com.btello.quoridor.presentation.online.create.CreateMatchOnlinePhase
import com.btello.quoridor.presentation.online.create.CreateMatchOnlineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateMatchOnlineViewModelTest {

    private fun viewModel(
        repo: FakeOnlineGameRepository,
        scope: CoroutineScope,
        name: String = "Ana",
    ) = CreateMatchOnlineViewModel(repo, scope, FakePlayerNameRepository(name))

    @Test
    fun `creating a match waits for the opponent and then starts as host`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        assertEquals(CreateMatchOnlinePhase.WaitingForOpponent, vm.uiState.phase)
        val code = vm.uiState.hostedCode
        assertEquals("ROOM01", code)
        assertTrue(effects.isEmpty())

        repo.simulateJoin(MatchId(code!!))
        advanceUntilIdle()

        val start = assertIs<OnlineSideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.HOST, start.setup.online?.slot)
        assertEquals(2, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `a four player match waits until all players join`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineSideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(CreateMatchOnlineEvent.PlayerCountChanged(4))
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        val id = MatchId(vm.uiState.hostedCode!!)
        repo.simulateJoin(id)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.joinedCount)
        assertTrue(effects.isEmpty())

        repo.simulateJoin(id)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.joinedCount)
        assertTrue(effects.isEmpty())

        repo.simulateJoin(id)
        advanceUntilIdle()

        val start = assertIs<OnlineSideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.HOST, start.setup.online?.slot)
        assertEquals(4, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `creating a match persists nothing but uses the loaded name`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = CreateMatchOnlineViewModel(repo, this, FakePlayerNameRepository("Bruno"))

        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        val code = vm.uiState.hostedCode!!
        assertEquals("Bruno", repo.current(MatchId(code)).playerNames.single())

        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `create is blocked while the name is blank`() = runTest {
        val vm = CreateMatchOnlineViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository())
        assertFalse(vm.uiState.canCreate)
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()
        assertNull(vm.uiState.hostedCode)
    }

    @Test
    fun `hosted code is exposed while waiting for the opponent`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        assertEquals("ROOM01", vm.uiState.hostedCode)
        assertEquals(CreateMatchOnlinePhase.WaitingForOpponent, vm.uiState.phase)
        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `cancel resets to idle and clears hosted code`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()
        assertEquals(CreateMatchOnlinePhase.WaitingForOpponent, vm.uiState.phase)

        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()

        assertEquals(CreateMatchOnlinePhase.Idle, vm.uiState.phase)
        assertNull(vm.uiState.hostedCode)
    }

    @Test
    fun `missing repository reports unsupported`() = runTest {
        val vm = CreateMatchOnlineViewModel(
            repository = null,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
        )
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()
        assertEquals(OnlineError.Unsupported, vm.uiState.error)
    }

    @Test
    fun `creating a public match advertises it in the open list`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.VisibilityChanged(true))
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        val open = repo.observeOpenMatches(AppConfig.VERSION).first()
        assertEquals(1, open.size)
        assertTrue(open.single().isPublic)
        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `creating a private match keeps it out of the open list`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.VisibilityChanged(false))
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        assertTrue(repo.observeOpenMatches(AppConfig.VERSION).first().isEmpty())
        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `competitive options are carried into the created match`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.FormatChanged(SeriesFormat.FIRST_TO_5))
        vm.onEvent(CreateMatchOnlineEvent.TimeControlChanged(10))
        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        val competitive = repo.current(MatchId(vm.uiState.hostedCode!!)).competitive
        assertEquals(SeriesFormat.FIRST_TO_5, competitive.config.format)
        assertEquals(600, competitive.config.timeControlSeconds)
        assertEquals(listOf(600_000L, 600_000L), competitive.remainingMillis)
        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `no timer by default leaves the match without a clock`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(CreateMatchOnlineEvent.CreateMatch)
        advanceUntilIdle()

        val competitive = repo.current(MatchId(vm.uiState.hostedCode!!)).competitive
        assertEquals(SeriesFormat.SINGLE, competitive.config.format)
        assertNull(competitive.config.timeControlSeconds)
        assertFalse(competitive.config.isCompetitive)
        vm.onEvent(CreateMatchOnlineEvent.Cancel)
        advanceUntilIdle()
    }
}
