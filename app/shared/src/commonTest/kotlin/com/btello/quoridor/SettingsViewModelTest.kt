package com.btello.quoridor

import com.btello.quoridor.presentation.settings.SettingsEvent
import com.btello.quoridor.presentation.settings.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @Test
    fun `loads stored player name on creation`() {
        val vm = SettingsViewModel(FakePlayerNameRepository("Bruno"))
        assertEquals("Bruno", vm.uiState.playerName)
    }

    @Test
    fun `empty repository yields blank player name`() {
        val vm = SettingsViewModel(FakePlayerNameRepository())
        assertEquals("", vm.uiState.playerName)
        assertFalse(vm.uiState.showNameDialog)
        assertFalse(vm.uiState.showLanguageDialog)
    }

    @Test
    fun `show and dismiss name dialog toggles state`() {
        val vm = SettingsViewModel(FakePlayerNameRepository())

        vm.onEvent(SettingsEvent.ShowNameDialog)
        assertTrue(vm.uiState.showNameDialog)

        vm.onEvent(SettingsEvent.DismissNameDialog)
        assertFalse(vm.uiState.showNameDialog)
    }

    @Test
    fun `show and dismiss language dialog toggles state`() {
        val vm = SettingsViewModel(FakePlayerNameRepository())

        vm.onEvent(SettingsEvent.ShowLanguageDialog)
        assertTrue(vm.uiState.showLanguageDialog)

        vm.onEvent(SettingsEvent.DismissLanguageDialog)
        assertFalse(vm.uiState.showLanguageDialog)
    }

    @Test
    fun `confirm name persists trimmed value and closes dialog`() {
        val repo = FakePlayerNameRepository()
        val vm = SettingsViewModel(repo)
        vm.onEvent(SettingsEvent.ShowNameDialog)

        vm.onEvent(SettingsEvent.ConfirmName("  Ana  "))

        assertEquals("Ana", vm.uiState.playerName)
        assertEquals("Ana", repo.name())
        assertFalse(vm.uiState.showNameDialog)
    }

    @Test
    fun `confirm blank name stores empty and closes dialog`() {
        val repo = FakePlayerNameRepository("Old")
        val vm = SettingsViewModel(repo)

        vm.onEvent(SettingsEvent.ConfirmName("   "))

        assertEquals("", vm.uiState.playerName)
        assertEquals("", repo.name())
        assertFalse(vm.uiState.showNameDialog)
    }
}
