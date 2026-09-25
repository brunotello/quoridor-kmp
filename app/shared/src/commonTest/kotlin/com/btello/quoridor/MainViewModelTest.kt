package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.main.DifficultyOption
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.MainEvent
import com.btello.quoridor.presentation.main.MainSideEffect
import com.btello.quoridor.presentation.main.MainViewModel
import com.btello.quoridor.presentation.main.PlayerSetupOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainViewModelTest {

    private val vm = MainViewModel()

    @Test
    fun `selecting local mode starts a game directly`() {
        val game = assertIs<MainSideEffect.NavigateToGame>(vm.effectFor(MainEvent.SelectMode(LOCAL_1V1)))
        assertEquals(2, game.setup.config.playerCount)
        assertTrue(game.setup.aiPlayers.isEmpty())
    }

    @Test
    fun `selecting versus ai navigates to difficulty with one ai`() {
        val difficulty = assertIs<MainSideEffect.NavigateToDifficulty>(
            vm.effectFor(MainEvent.SelectMode(VERSUS_AI)),
        )
        assertEquals(VERSUS_AI, difficulty.mode)
        assertEquals(1, difficulty.aiCount)
    }

    @Test
    fun `selecting four players navigates to the player setup submenu`() {
        val setup = assertIs<MainSideEffect.NavigateToPlayerSetup>(
            vm.effectFor(MainEvent.SelectMode(FOUR_PLAYERS)),
        )
        assertEquals(FOUR_PLAYERS, setup.mode)
    }

    @Test
    fun `four players all humans starts a four player game without ai`() {
        val game = assertIs<MainSideEffect.NavigateToGame>(
            vm.effectFor(MainEvent.SelectPlayerSetup(FOUR_PLAYERS, PlayerSetupOption.ALL_HUMANS)),
        )
        assertEquals(4, game.setup.config.playerCount)
        assertTrue(game.setup.aiPlayers.isEmpty())
        assertNull(game.setup.difficulty)
    }

    @Test
    fun `four players with ai navigates to difficulty carrying the ai count`() {
        val difficulty = assertIs<MainSideEffect.NavigateToDifficulty>(
            vm.effectFor(MainEvent.SelectPlayerSetup(FOUR_PLAYERS, PlayerSetupOption.THREE_AI)),
        )
        assertEquals(FOUR_PLAYERS, difficulty.mode)
        assertEquals(3, difficulty.aiCount)
    }

    @Test
    fun `choosing a difficulty for four players starts the game with the selected ai`() {
        val game = assertIs<MainSideEffect.NavigateToGame>(
            vm.effectFor(MainEvent.SelectDifficulty(FOUR_PLAYERS, aiCount = 2, option = DifficultyOption.HARD)),
        )
        assertEquals(4, game.setup.config.playerCount)
        assertEquals(setOf(PlayerId(2), PlayerId(3)), game.setup.aiPlayers)
        assertEquals(AiDifficulty.HARD, game.setup.difficulty)
    }

    @Test
    fun `selecting online navigates to the online lobby`() {
        assertIs<MainSideEffect.NavigateToOnlineLobby>(
            vm.effectFor(MainEvent.SelectMode(com.btello.quoridor.presentation.main.GameMode.ONLINE)),
        )
    }

    @Test
    fun `every offered mode is enabled and produces an effect`() {
        for (mode in com.btello.quoridor.presentation.main.GameMode.entries) {
            assertTrue(mode.enabled)
            assertIs<MainSideEffect>(vm.effectFor(MainEvent.SelectMode(mode)))
        }
    }
}
