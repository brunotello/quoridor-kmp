package com.btello.quoridor

import com.btello.quoridor.presentation.main.PlayerSetupOption
import com.btello.quoridor.presentation.main.PlayerSetupOption.ALL_HUMANS
import com.btello.quoridor.presentation.main.PlayerSetupOption.ONE_AI
import com.btello.quoridor.presentation.main.PlayerSetupOption.THREE_AI
import com.btello.quoridor.presentation.main.PlayerSetupOption.TWO_AI
import com.btello.quoridor.presentation.main.requiresDifficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerSetupOptionTest {

    @Test
    fun `each option maps to its ai count`() {
        assertEquals(0, ALL_HUMANS.aiCount)
        assertEquals(1, ONE_AI.aiCount)
        assertEquals(2, TWO_AI.aiCount)
        assertEquals(3, THREE_AI.aiCount)
    }

    @Test
    fun `only options with ai players require choosing a difficulty`() {
        assertFalse(ALL_HUMANS.requiresDifficulty)
        assertTrue(ONE_AI.requiresDifficulty)
        assertTrue(TWO_AI.requiresDifficulty)
        assertTrue(THREE_AI.requiresDifficulty)
    }

    @Test
    fun `exposes the options from zero to three ai`() {
        assertEquals(listOf(ALL_HUMANS, ONE_AI, TWO_AI, THREE_AI), PlayerSetupOption.entries)
    }

    @Test
    fun `ai counts never exceed the three non-human seats`() {
        assertTrue(PlayerSetupOption.entries.all { it.aiCount in 0..3 })
    }
}
