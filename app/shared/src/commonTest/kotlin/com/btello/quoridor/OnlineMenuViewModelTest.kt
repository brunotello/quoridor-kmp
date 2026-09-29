package com.btello.quoridor

import com.btello.quoridor.presentation.online.menu.OnlineMenuEvent
import com.btello.quoridor.presentation.online.menu.OnlineMenuViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnlineMenuViewModelTest {

    @Test
    fun `starts unconfirmed when no name is stored`() {
        val vm = OnlineMenuViewModel(FakePlayerNameRepository())
        assertFalse(vm.uiState.nameConfirmed)
        assertEquals("", vm.uiState.playerName)
    }

    @Test
    fun `starts confirmed when a name is already stored`() {
        val vm = OnlineMenuViewModel(FakePlayerNameRepository("Saved"))
        assertTrue(vm.uiState.nameConfirmed)
        assertEquals("Saved", vm.uiState.playerName)
    }

    @Test
    fun `confirming the name persists it trimmed and advances`() {
        val names = FakePlayerNameRepository()
        val vm = OnlineMenuViewModel(names)
        vm.onEvent(OnlineMenuEvent.NameChanged("  Bruno  "))

        vm.onEvent(OnlineMenuEvent.ConfirmName)

        assertTrue(vm.uiState.nameConfirmed)
        assertEquals("Bruno", vm.uiState.playerName)
        assertEquals("Bruno", names.name())
    }

    @Test
    fun `confirming a blank name keeps the user on the name step`() {
        val names = FakePlayerNameRepository()
        val vm = OnlineMenuViewModel(names)
        vm.onEvent(OnlineMenuEvent.NameChanged("   "))

        vm.onEvent(OnlineMenuEvent.ConfirmName)

        assertFalse(vm.uiState.nameConfirmed)
        assertEquals("", names.name())
    }

    @Test
    fun `editing the name updates the state`() {
        val vm = OnlineMenuViewModel(FakePlayerNameRepository())
        vm.onEvent(OnlineMenuEvent.NameChanged("Ana"))
        assertEquals("Ana", vm.uiState.playerName)
        assertTrue(vm.uiState.canConfirmName)
    }
}
