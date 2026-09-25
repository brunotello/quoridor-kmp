package com.btello.quoridor.presentation.main

import org.jetbrains.compose.resources.StringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.player_setup_humans_description
import quoridor.app.shared.generated.resources.player_setup_humans_title
import quoridor.app.shared.generated.resources.player_setup_one_ai_description
import quoridor.app.shared.generated.resources.player_setup_one_ai_title
import quoridor.app.shared.generated.resources.player_setup_three_ai_description
import quoridor.app.shared.generated.resources.player_setup_three_ai_title
import quoridor.app.shared.generated.resources.player_setup_two_ai_description
import quoridor.app.shared.generated.resources.player_setup_two_ai_title

/**
 * Opciones del submenú de configuración de la partida de 4 jugadores: cuántos de
 * los 4 peones controla la IA (0 a 3). El resto son humanos locales que comparten
 * el dispositivo. Agregar/ajustar una opción sólo requiere una entrada aquí (más
 * sus strings).
 */
internal enum class PlayerSetupOption(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val aiCount: Int,
) {
    ALL_HUMANS(
        titleRes = Res.string.player_setup_humans_title,
        descriptionRes = Res.string.player_setup_humans_description,
        aiCount = 0,
    ),
    ONE_AI(
        titleRes = Res.string.player_setup_one_ai_title,
        descriptionRes = Res.string.player_setup_one_ai_description,
        aiCount = 1,
    ),
    TWO_AI(
        titleRes = Res.string.player_setup_two_ai_title,
        descriptionRes = Res.string.player_setup_two_ai_description,
        aiCount = 2,
    ),
    THREE_AI(
        titleRes = Res.string.player_setup_three_ai_title,
        descriptionRes = Res.string.player_setup_three_ai_description,
        aiCount = 3,
    ),
}

/** True si la opción incorpora al menos un jugador IA (y por tanto requiere dificultad). */
internal val PlayerSetupOption.requiresDifficulty: Boolean
    get() = aiCount > 0
