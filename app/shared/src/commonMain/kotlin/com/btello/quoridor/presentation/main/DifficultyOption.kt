package com.btello.quoridor.presentation.main

import com.btello.quoridor.domain.ai.AiDifficulty
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.difficulty_easy
import quoridor.app.shared.generated.resources.difficulty_easy_description
import quoridor.app.shared.generated.resources.difficulty_expert
import quoridor.app.shared.generated.resources.difficulty_expert_description
import quoridor.app.shared.generated.resources.difficulty_hard
import quoridor.app.shared.generated.resources.difficulty_hard_description
import quoridor.app.shared.generated.resources.difficulty_medium
import quoridor.app.shared.generated.resources.difficulty_medium_description

/**
 * Niveles de dificultad ofrecidos en el submenú del modo IA.
 *
 * Cada opción declara su título, descripción, número de estrellas y la [AiDifficulty] de
 * dominio con la que arranca la partida. Agregar un nivel sólo requiere una
 * entrada aquí (más sus strings).
 */
internal enum class DifficultyOption(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val stars: Int,
    val difficulty: AiDifficulty,
) {
    EASY(
        titleRes = Res.string.difficulty_easy,
        descriptionRes = Res.string.difficulty_easy_description,
        stars = 1,
        difficulty = AiDifficulty.EASY,
    ),
    MEDIUM(
        titleRes = Res.string.difficulty_medium,
        descriptionRes = Res.string.difficulty_medium_description,
        stars = 2,
        difficulty = AiDifficulty.MEDIUM,
    ),
    HARD(
        titleRes = Res.string.difficulty_hard,
        descriptionRes = Res.string.difficulty_hard_description,
        stars = 3,
        difficulty = AiDifficulty.HARD,
    ),
    EXPERT(
        titleRes = Res.string.difficulty_expert,
        descriptionRes = Res.string.difficulty_expert_description,
        stars = 4,
        difficulty = AiDifficulty.EXPERT,
    ),
}
