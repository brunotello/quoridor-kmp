package com.btello.quoridor

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.MatchStatus
import com.btello.quoridor.domain.online.PlayerSlot
import com.btello.quoridor.presentation.online.OnlineLobbyError
import com.btello.quoridor.presentation.online.OnlineLobbyEvent
import com.btello.quoridor.presentation.online.OnlineLobbyPhase
import com.btello.quoridor.presentation.online.OnlineLobbySideEffect
import com.btello.quoridor.presentation.online.OnlineLobbyStep
import com.btello.quoridor.presentation.online.OnlineLobbyViewModel
import kotlinx.coroutines.CoroutineScope
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

class OnlineLobbyViewModelTest {

    private fun viewModel(
        repo: FakeOnlineGameRepository,
        scope: CoroutineScope,
        name: String = "Ana",
    ): OnlineLobbyViewModel {
        val vm = OnlineLobbyViewModel(repo, scope, FakePlayerNameRepository())
        vm.onEvent(OnlineLobbyEvent.NameChanged(name))
        return vm
    }

    @Test
    fun `creating a match waits for the opponent and then starts as host`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineLobbySideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()

        assertEquals(OnlineLobbyPhase.WaitingForOpponent, vm.uiState.phase)
        val code = vm.uiState.hostedCode
        assertEquals("ROOM01", code)
        assertTrue(effects.isEmpty())

        repo.simulateJoin(MatchId(code!!))
        advanceUntilIdle()

        val start = assertIs<OnlineLobbySideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.HOST, start.setup.online?.slot)
        assertEquals(2, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `a four player match waits until all players join`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineLobbySideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(OnlineLobbyEvent.PlayerCountChanged(4))
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
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

        val start = assertIs<OnlineLobbySideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.HOST, start.setup.online?.slot)
        assertEquals(4, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `joining an existing match starts on the assigned slot`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = repo.createMatch(GameConfig(playerCount = 4), "Host")
        repo.simulateJoin(id)
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineLobbySideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(OnlineLobbyEvent.JoinCodeChanged(id.value))
        vm.onEvent(OnlineLobbyEvent.JoinMatch)
        advanceUntilIdle()

        val start = assertIs<OnlineLobbySideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot(2), start.setup.online?.slot)
        assertEquals(id, start.setup.online?.matchId)
        assertEquals(4, start.setup.config.playerCount)
        collector.cancel()
    }

    @Test
    fun `creating a match persists the player name`() = runTest {
        val repo = FakeOnlineGameRepository()
        val names = FakePlayerNameRepository()
        val vm = OnlineLobbyViewModel(repo, this, names)
        vm.onEvent(OnlineLobbyEvent.NameChanged("  Bruno  "))

        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()

        val code = vm.uiState.hostedCode!!
        assertEquals("Bruno", names.name())
        assertEquals("Bruno", repo.current(MatchId(code)).playerNames.single())

        vm.onEvent(OnlineLobbyEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `the stored name is preloaded into the state`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository("Saved"))
        assertEquals("Saved", vm.uiState.playerName)
        assertTrue(vm.uiState.canCreate)
    }

    @Test
    fun `create is blocked while the name is blank`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository())
        assertFalse(vm.uiState.canCreate)
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()
        assertNull(vm.uiState.hostedCode)
    }

    @Test
    fun `joining an unknown code surfaces a not found error`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.JoinCodeChanged("NOPE12"))
        vm.onEvent(OnlineLobbyEvent.JoinMatch)
        advanceUntilIdle()

        assertEquals(OnlineLobbyError.NotFound, vm.uiState.error)
        assertEquals(OnlineLobbyPhase.Idle, vm.uiState.phase)
    }

    @Test
    fun `join code is trimmed and uppercased`() = runTest {
        val vm = viewModel(FakeOnlineGameRepository(), this)
        vm.onEvent(OnlineLobbyEvent.JoinCodeChanged("  room01 "))
        assertEquals("ROOM01", vm.uiState.joinCode)
    }

    @Test
    fun `cancel resets to idle and clears hosted code`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()
        assertEquals(OnlineLobbyPhase.WaitingForOpponent, vm.uiState.phase)

        vm.onEvent(OnlineLobbyEvent.Cancel)
        advanceUntilIdle()

        assertEquals(OnlineLobbyPhase.Idle, vm.uiState.phase)
        assertNull(vm.uiState.hostedCode)
    }

    @Test
    fun `missing repository reports unsupported`() = runTest {
        val vm = OnlineLobbyViewModel(
            repository = null,
            scope = this,
            playerNameRepository = FakePlayerNameRepository("Ana"),
        )
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()
        assertEquals(OnlineLobbyError.Unsupported, vm.uiState.error)
    }

    @Test
    fun `starts on the name step when no name is stored`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository())
        assertEquals(OnlineLobbyStep.Name, vm.uiState.step)
    }

    @Test
    fun `starts on the menu step when a name is already stored`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository("Saved"))
        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)
    }

    @Test
    fun `confirming the name persists it and advances to the menu`() = runTest {
        val names = FakePlayerNameRepository()
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, names)
        vm.onEvent(OnlineLobbyEvent.NameChanged("  Bruno  "))

        vm.onEvent(OnlineLobbyEvent.ConfirmName)

        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)
        assertEquals("Bruno", vm.uiState.playerName)
        assertEquals("Bruno", names.name())
    }

    @Test
    fun `confirming a blank name keeps the user on the name step`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository())
        vm.onEvent(OnlineLobbyEvent.NameChanged("   "))

        vm.onEvent(OnlineLobbyEvent.ConfirmName)

        assertEquals(OnlineLobbyStep.Name, vm.uiState.step)
    }

    @Test
    fun `choosing an action opens its step and back returns to the menu`() = runTest {
        val vm = OnlineLobbyViewModel(FakeOnlineGameRepository(), this, FakePlayerNameRepository("Ana"))

        vm.onEvent(OnlineLobbyEvent.ChooseCreate)
        assertEquals(OnlineLobbyStep.Create, vm.uiState.step)
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)

        vm.onEvent(OnlineLobbyEvent.ChooseJoin)
        assertEquals(OnlineLobbyStep.Join, vm.uiState.step)
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)
    }

    @Test
    fun `navigating back from the waiting room stops hosting and returns to the menu`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = OnlineLobbyViewModel(repo, this, FakePlayerNameRepository("Ana"))

        vm.onEvent(OnlineLobbyEvent.ChooseCreate)
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()
        val id = MatchId(vm.uiState.hostedCode!!)
        assertEquals(OnlineLobbyPhase.WaitingForOpponent, vm.uiState.phase)

        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()

        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)
        assertEquals(OnlineLobbyPhase.Idle, vm.uiState.phase)
        assertNull(vm.uiState.hostedCode)
        assertEquals(MatchStatus.ABANDONED, repo.current(id).status)
    }
}
