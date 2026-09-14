package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.main.GameMode
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.requiresDifficulty
import com.btello.quoridor.presentation.main.toGameConfig
import com.btello.quoridor.presentation.main.toGameSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameModeTest {

    @Test
    fun localOneVsOneIsEnabledAndStartsATwoPlayerGame() {
        assertTrue(LOCAL_1V1.enabled)
        assertEquals(2, LOCAL_1V1.toGameConfig().playerCount)
    }

    @Test
    fun localOneVsOneStartsDirectlyWithoutAi() {
        assertFalse(LOCAL_1V1.requiresDifficulty)
        val setup = LOCAL_1V1.toGameSetup()
        assertTrue(setup.aiPlayers.isEmpty())
        assertNull(setup.difficulty)
    }

    @Test
    fun versusAiIsEnabledAndRequiresDifficulty() {
        assertTrue(VERSUS_AI.enabled)
        assertTrue(VERSUS_AI.requiresDifficulty)
    }

    @Test
    fun versusAiSetupMarksSecondPlayerAsAiWithChosenDifficulty() {
        val setup = VERSUS_AI.toGameSetup(AiDifficulty.HARD)
        assertEquals(setOf(PlayerId(1)), setup.aiPlayers)
        assertEquals(AiDifficulty.HARD, setup.difficulty)
        assertEquals(2, setup.config.playerCount)
    }

    @Test
    fun exposesEveryModeInDeclarationOrder() {
        assertEquals(listOf(VERSUS_AI, LOCAL_1V1), GameMode.entries)
    }
}
