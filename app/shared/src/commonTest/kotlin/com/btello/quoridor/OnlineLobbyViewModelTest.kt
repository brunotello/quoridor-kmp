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
        val id = repo.createMatch(GameConfig(playerCount = 4), "Host", AppConfig.VERSION)
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
    fun `hosted code is exposed while waiting for the opponent`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()

        assertEquals("ROOM01", vm.uiState.hostedCode)
        assertEquals(OnlineLobbyPhase.WaitingForOpponent, vm.uiState.phase)
        vm.onEvent(OnlineLobbyEvent.Cancel)
        advanceUntilIdle()
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
    fun `creating a public match advertises it in the open list`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.VisibilityChanged(true))
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()

        val open = repo.observeOpenMatches(AppConfig.VERSION).first()
        assertEquals(1, open.size)
        assertTrue(open.single().isPublic)
        vm.onEvent(OnlineLobbyEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `creating a private match keeps it out of the open list`() = runTest {
        val repo = FakeOnlineGameRepository()
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.VisibilityChanged(false))
        vm.onEvent(OnlineLobbyEvent.CreateMatch)
        advanceUntilIdle()

        assertTrue(repo.observeOpenMatches(AppConfig.VERSION).first().isEmpty())
        vm.onEvent(OnlineLobbyEvent.Cancel)
        advanceUntilIdle()
    }

    @Test
    fun `browsing lists the available public matches`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto", playerCount = 2)
        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro", playerCount = 4)
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()

        assertEquals(OnlineLobbyStep.Browse, vm.uiState.step)
        val codes = vm.uiState.openMatches.map { it.id.value }
        assertEquals(listOf("PUB001", "PUB002"), codes)
        assertEquals("Beto", vm.uiState.openMatches.first().hostName)
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()
    }

    @Test
    fun `the open list updates as matches fill up`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", playerCount = 2)
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.openMatches.size)

        repo.simulateJoin(id)
        advanceUntilIdle()

        assertTrue(vm.uiState.openMatches.isEmpty())
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()
    }

    @Test
    fun `joining a public match starts on the assigned slot without a code`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", playerCount = 2)
        val vm = viewModel(repo, this)
        val effects = mutableListOf<OnlineLobbySideEffect>()
        val collector = launch { vm.sideEffects.toList(effects) }

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()
        vm.onEvent(OnlineLobbyEvent.JoinPublicMatch(id))
        advanceUntilIdle()

        val start = assertIs<OnlineLobbySideEffect.StartGame>(effects.single())
        assertEquals(PlayerSlot.GUEST, start.setup.online?.slot)
        assertEquals(id, start.setup.online?.matchId)
        collector.cancel()
    }

    @Test
    fun `refreshing the browse list restarts the observation`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto")
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.openMatches.size)

        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro")
        vm.onEvent(OnlineLobbyEvent.RefreshBrowse)
        advanceUntilIdle()

        assertEquals(listOf("PUB001", "PUB002"), vm.uiState.openMatches.map { it.id.value })
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()
    }

    @Test
    fun `back from browsing returns to the menu and clears the list`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto")
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()
        assertTrue(vm.uiState.openMatches.isNotEmpty())

        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()

        assertEquals(OnlineLobbyStep.Menu, vm.uiState.step)
        assertTrue(vm.uiState.openMatches.isEmpty())
    }

    @Test
    fun `browsing hides public matches created with a different app version`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto", appVersion = AppConfig.VERSION)
        repo.seedPublicMatch(MatchId("PUB002"), hostName = "Caro", appVersion = "0.0.1")
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()

        assertEquals(listOf("PUB001"), vm.uiState.openMatches.map { it.id.value })
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()
    }

    @Test
    fun `joining a public match leaves browse and clears the list`() = runTest {
        val repo = FakeOnlineGameRepository()
        repo.seedPublicMatch(MatchId("PUB001"), hostName = "Beto")
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.ChooseBrowse)
        advanceUntilIdle()
        vm.onEvent(OnlineLobbyEvent.RefreshBrowse)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.openMatches.size)
        assertEquals(OnlineLobbyStep.Browse, vm.uiState.step)
        vm.onEvent(OnlineLobbyEvent.NavigateBack)
        advanceUntilIdle()
    }

    @Test
    fun `joining a match with an incompatible version surfaces a version error`() = runTest {
        val repo = FakeOnlineGameRepository()
        val id = MatchId("PUB001")
        repo.seedPublicMatch(id, hostName = "Beto", appVersion = "0.0.1")
        val vm = viewModel(repo, this)

        vm.onEvent(OnlineLobbyEvent.JoinCodeChanged(id.value))
        vm.onEvent(OnlineLobbyEvent.JoinMatch)
        advanceUntilIdle()

        assertEquals(OnlineLobbyError.IncompatibleVersion, vm.uiState.error)
        assertEquals(OnlineLobbyPhase.Idle, vm.uiState.phase)
    }
}
