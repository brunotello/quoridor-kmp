package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.LOCAL_AI_PLAYER_COUNT_OPTIONS
import com.btello.quoridor.presentation.main.LocalMatchSetupState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMatchSetupTest {

    @Test
    fun versusAiOffersPlayerCountAndDifficultyButNotTimer() {
        val state = LocalMatchSetupState(VERSUS_AI)
        assertTrue(state.showPlayerCount)
        assertTrue(state.showDifficulty)
        assertFalse(state.showTurnTime)
        assertEquals(2, state.playerCount)
    }

    @Test
    fun humanModesOfferTimerButNotPlayerCountOrDifficulty() {
        for (state in listOf(LocalMatchSetupState(LOCAL_1V1), LocalMatchSetupState(FOUR_PLAYERS))) {
            assertFalse(state.showPlayerCount)
            assertFalse(state.showDifficulty)
            assertTrue(state.showTurnTime)
        }
    }

    @Test
    fun aiPlayerCountOptionsAreTwoAndFour() {
        assertEquals(listOf(2, 4), LOCAL_AI_PLAYER_COUNT_OPTIONS)
    }

    @Test
    fun versusAiTwoPlayersPutsOneHumanAgainstOneAi() {
        val setup = LocalMatchSetupState(VERSUS_AI, playerCount = 2, difficulty = AiDifficulty.HARD).toGameSetup()
        assertEquals(2, setup.config.playerCount)
        assertEquals(setOf(PlayerId(1)), setup.aiPlayers)
        assertEquals(AiDifficulty.HARD, setup.difficulty)
    }

    @Test
    fun versusAiFourPlayersPutsOneHumanAgainstThreeAi() {
        val setup = LocalMatchSetupState(VERSUS_AI, playerCount = 4, difficulty = AiDifficulty.EXPERT).toGameSetup()
        assertEquals(4, setup.config.playerCount)
        assertEquals(setOf(PlayerId(1), PlayerId(2), PlayerId(3)), setup.aiPlayers)
        assertEquals(AiDifficulty.EXPERT, setup.difficulty)
    }

    @Test
    fun versusAiDropsTheTurnTimerEvenIfSet() {
        val setup = LocalMatchSetupState(
            VERSUS_AI,
            format = SeriesFormat.FIRST_TO_3,
            turnTimeSeconds = 30,
        ).toGameSetup()
        assertEquals(SeriesFormat.FIRST_TO_3, setup.competitive.format)
        assertNull(setup.competitive.turnTimeSeconds)
    }

    @Test
    fun oneVsOneHasNoAiAndKeepsCompetitiveConfig() {
        val setup = LocalMatchSetupState(
            LOCAL_1V1,
            format = SeriesFormat.FIRST_TO_5,
            turnTimeSeconds = 45,
        ).toGameSetup()
        assertEquals(2, setup.config.playerCount)
        assertTrue(setup.aiPlayers.isEmpty())
        assertNull(setup.difficulty)
        assertEquals(SeriesFormat.FIRST_TO_5, setup.competitive.format)
        assertEquals(45, setup.competitive.turnTimeSeconds)
    }

    @Test
    fun fourPlayersAreAllHumanWithCompetitiveConfig() {
        val setup = LocalMatchSetupState(
            FOUR_PLAYERS,
            format = SeriesFormat.FIRST_TO_3,
            turnTimeSeconds = 60,
        ).toGameSetup()
        assertEquals(4, setup.config.playerCount)
        assertTrue(setup.aiPlayers.isEmpty())
        assertNull(setup.difficulty)
        assertEquals(60, setup.competitive.turnTimeSeconds)
    }
}
