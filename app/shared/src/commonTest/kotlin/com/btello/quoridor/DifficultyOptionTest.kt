package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.presentation.main.DifficultyOption
import com.btello.quoridor.presentation.main.DifficultyOption.EASY
import com.btello.quoridor.presentation.main.DifficultyOption.EXPERT
import com.btello.quoridor.presentation.main.DifficultyOption.HARD
import com.btello.quoridor.presentation.main.DifficultyOption.MEDIUM
import com.btello.quoridor.presentation.main.labelRes
import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyOptionTest {

    @Test
    fun `each option maps to its domain difficulty`() {
        assertEquals(AiDifficulty.EASY, EASY.difficulty)
        assertEquals(AiDifficulty.MEDIUM, MEDIUM.difficulty)
        assertEquals(AiDifficulty.HARD, HARD.difficulty)
        assertEquals(AiDifficulty.EXPERT, EXPERT.difficulty)
    }

    @Test
    fun `exposes the levels from easiest to hardest`() {
        assertEquals(listOf(EASY, MEDIUM, HARD, EXPERT), DifficultyOption.entries)
    }

    @Test
    fun `covers every domain difficulty level`() {
        assertEquals(
            AiDifficulty.entries.toSet(),
            DifficultyOption.entries.map { it.difficulty }.toSet(),
        )
    }

    @Test
    fun `labelRes maps each difficulty to its option title`() {
        for (option in DifficultyOption.entries) {
            assertEquals(option.titleRes, option.difficulty.labelRes())
        }
    }
}
