package com.btello.quoridor

import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.main.GameMode
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.ONLINE
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.aiPlayersForCount
import com.btello.quoridor.presentation.main.requiresLobby
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameModeTest {

    @Test
    fun localModesAreEnabledWithTheirStartingPlayerCount() {
        assertTrue(VERSUS_AI.enabled)
        assertEquals(2, VERSUS_AI.playerCount)
        assertTrue(LOCAL_1V1.enabled)
        assertEquals(2, LOCAL_1V1.playerCount)
        assertTrue(FOUR_PLAYERS.enabled)
        assertEquals(4, FOUR_PLAYERS.playerCount)
    }

    @Test
    fun onlyOnlineRequiresTheLobby() {
        assertTrue(ONLINE.requiresLobby)
        assertFalse(VERSUS_AI.requiresLobby)
        assertFalse(LOCAL_1V1.requiresLobby)
        assertFalse(FOUR_PLAYERS.requiresLobby)
    }

    @Test
    fun aiPlayersForCountTakesTheHighestIdsLeavingPlayerZeroHuman() {
        assertEquals(emptySet(), aiPlayersForCount(4, 0))
        assertEquals(setOf(PlayerId(1)), aiPlayersForCount(2, 1))
        assertEquals(setOf(PlayerId(3)), aiPlayersForCount(4, 1))
        assertEquals(setOf(PlayerId(2), PlayerId(3)), aiPlayersForCount(4, 2))
        assertEquals(setOf(PlayerId(1), PlayerId(2), PlayerId(3)), aiPlayersForCount(4, 3))
        assertFalse(PlayerId(0) in aiPlayersForCount(4, 3))
    }

    @Test
    fun exposesEveryModeInDeclarationOrder() {
        assertEquals(listOf(VERSUS_AI, LOCAL_1V1, FOUR_PLAYERS, ONLINE), GameMode.entries)
    }

    @Test
    fun everyModeDeclaresADistinctEmoji() {
        val emojis = GameMode.entries.map { it.emojiRes }
        assertEquals(emojis.size, emojis.toSet().size)
    }
}
