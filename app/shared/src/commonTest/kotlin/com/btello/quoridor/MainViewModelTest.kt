package com.btello.quoridor

import com.btello.quoridor.presentation.main.GameMode
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.ONLINE
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.MainEvent
import com.btello.quoridor.presentation.main.MainSideEffect
import com.btello.quoridor.presentation.main.MainViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MainViewModelTest {

    private val vm = MainViewModel()

    @Test
    fun `selecting versus ai opens the local setup for that mode`() {
        val setup = assertIs<MainSideEffect.NavigateToLocalSetup>(
            vm.effectFor(MainEvent.SelectMode(VERSUS_AI)),
        )
        assertEquals(VERSUS_AI, setup.mode)
    }

    @Test
    fun `selecting one versus one opens the local setup for that mode`() {
        val setup = assertIs<MainSideEffect.NavigateToLocalSetup>(
            vm.effectFor(MainEvent.SelectMode(LOCAL_1V1)),
        )
        assertEquals(LOCAL_1V1, setup.mode)
    }

    @Test
    fun `selecting four players opens the local setup for that mode`() {
        val setup = assertIs<MainSideEffect.NavigateToLocalSetup>(
            vm.effectFor(MainEvent.SelectMode(FOUR_PLAYERS)),
        )
        assertEquals(FOUR_PLAYERS, setup.mode)
    }

    @Test
    fun `selecting online navigates to the online lobby`() {
        assertIs<MainSideEffect.NavigateToOnlineLobby>(
            vm.effectFor(MainEvent.SelectMode(ONLINE)),
        )
    }

    @Test
    fun `every offered mode is enabled and produces an effect`() {
        for (mode in GameMode.entries) {
            assertTrue(mode.enabled)
            assertIs<MainSideEffect>(vm.effectFor(MainEvent.SelectMode(mode)))
        }
    }
}
