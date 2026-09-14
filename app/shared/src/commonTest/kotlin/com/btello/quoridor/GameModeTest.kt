package com.btello.quoridor

import com.btello.quoridor.presentation.main.GameMode
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.main.toGameConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameModeTest {

    @Test
    fun localOneVsOneIsEnabledAndStartsATwoPlayerGame() {
        assertTrue(LOCAL_1V1.enabled)
        assertEquals(2, LOCAL_1V1.toGameConfig().playerCount)
    }

    @Test
    fun versusAiIsDisabledForNow() {
        assertFalse(VERSUS_AI.enabled)
    }

    @Test
    fun exposesEveryModeInDeclarationOrder() {
        assertEquals(listOf(VERSUS_AI, LOCAL_1V1), GameMode.entries)
    }
}
