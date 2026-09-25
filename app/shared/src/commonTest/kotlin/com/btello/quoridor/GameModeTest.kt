package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.main.GameMode
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.ONLINE
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.aiPlayersForCount
import com.btello.quoridor.presentation.main.buildGameSetup
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
        val setup = VERSUS_AI.toGameSetup(difficulty = AiDifficulty.HARD)
        assertEquals(setOf(PlayerId(1)), setup.aiPlayers)
        assertEquals(AiDifficulty.HARD, setup.difficulty)
        assertEquals(2, setup.config.playerCount)
    }

    @Test
    fun fourPlayersIsEnabledConfigurableAndDoesNotGoStraightToDifficulty() {
        assertTrue(FOUR_PLAYERS.enabled)
        assertTrue(FOUR_PLAYERS.configurableAi)
        assertFalse(FOUR_PLAYERS.requiresDifficulty)
        assertEquals(4, FOUR_PLAYERS.toGameConfig().playerCount)
    }

    @Test
    fun fourPlayersAllHumansHasNoAiAndNoDifficulty() {
        val setup = FOUR_PLAYERS.toGameSetup(aiCount = 0, difficulty = AiDifficulty.HARD)
        assertTrue(setup.aiPlayers.isEmpty())
        assertNull(setup.difficulty)
        assertEquals(4, setup.config.playerCount)
    }

    @Test
    fun fourPlayersWithThreeAiMarksTheLastThreePlayers() {
        val setup = FOUR_PLAYERS.toGameSetup(aiCount = 3, difficulty = AiDifficulty.MEDIUM)
        assertEquals(setOf(PlayerId(1), PlayerId(2), PlayerId(3)), setup.aiPlayers)
        assertEquals(AiDifficulty.MEDIUM, setup.difficulty)
    }

    @Test
    fun aiPlayersForCountTakesTheHighestIdsLeavingPlayerZeroHuman() {
        assertEquals(emptySet(), aiPlayersForCount(4, 0))
        assertEquals(setOf(PlayerId(3)), aiPlayersForCount(4, 1))
        assertEquals(setOf(PlayerId(2), PlayerId(3)), aiPlayersForCount(4, 2))
        assertEquals(setOf(PlayerId(1), PlayerId(2), PlayerId(3)), aiPlayersForCount(4, 3))
        assertFalse(PlayerId(0) in aiPlayersForCount(4, 3))
    }

    @Test
    fun buildGameSetupDropsDifficultyWhenThereAreNoAiPlayers() {
        val setup = buildGameSetup(4, emptySet(), AiDifficulty.EXPERT)
        assertNull(setup.difficulty)
    }

    @Test
    fun exposesEveryModeInDeclarationOrder() {
        assertEquals(listOf(VERSUS_AI, LOCAL_1V1, ONLINE, FOUR_PLAYERS), GameMode.entries)
    }

    @Test
    fun everyModeDeclaresADistinctEmoji() {
        val emojis = GameMode.entries.map { it.emojiRes }
        assertEquals(emojis.size, emojis.toSet().size)
    }
}
