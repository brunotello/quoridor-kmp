package com.btello.quoridor.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.ui.graphics.vector.ImageVector
import com.btello.quoridor.domain.ai.AiDifficulty
import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.difficulty_easy
import quoridor.app.shared.generated.resources.difficulty_easy_description
import quoridor.app.shared.generated.resources.difficulty_hard
import quoridor.app.shared.generated.resources.difficulty_hard_description
import quoridor.app.shared.generated.resources.difficulty_medium
import quoridor.app.shared.generated.resources.difficulty_medium_description

/**
 * Niveles de dificultad ofrecidos en el submenú del modo IA.
 *
 * Cada opción declara su título, descripción, icono y la [AiDifficulty] de
 * dominio con la que arranca la partida. Agregar un nivel sólo requiere una
 * entrada aquí (más sus strings).
 */
internal enum class DifficultyOption(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val difficulty: AiDifficulty,
) {
    EASY(
        titleRes = Res.string.difficulty_easy,
        descriptionRes = Res.string.difficulty_easy_description,
        icon = Icons.Outlined.SentimentVerySatisfied,
        difficulty = AiDifficulty.EASY,
    ),
    MEDIUM(
        titleRes = Res.string.difficulty_medium,
        descriptionRes = Res.string.difficulty_medium_description,
        icon = Icons.Outlined.SentimentNeutral,
        difficulty = AiDifficulty.MEDIUM,
    ),
    HARD(
        titleRes = Res.string.difficulty_hard,
        descriptionRes = Res.string.difficulty_hard_description,
        icon = Icons.Outlined.SentimentVeryDissatisfied,
        difficulty = AiDifficulty.HARD,
    ),
}
