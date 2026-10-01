package com.btello.quoridor.presentation.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.main.GameMode.FOUR_PLAYERS
import com.btello.quoridor.presentation.main.GameMode.LOCAL_1V1
import com.btello.quoridor.presentation.main.GameMode.VERSUS_AI
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.online.OptionChipSection
import com.btello.quoridor.presentation.online.PlayerCountChips
import com.btello.quoridor.presentation.online.SeriesFormatChips
import com.btello.quoridor.presentation.online.TurnTimeChips
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.match_setup_difficulty_label
import quoridor.app.shared.generated.resources.match_setup_start
import quoridor.app.shared.generated.resources.match_setup_title

/**
 * Pantalla de configuración de una partida local, con el mismo estilo de chips que
 * la creación de salas online. Según el [mode] muestra: cantidad de jugadores y
 * dificultad (contra la IA), o tiempo por turno (entre humanos); la cantidad de
 * rondas se ofrece siempre. Al confirmar arranca la partida con [onStart].
 */
@Composable
internal fun LocalMatchSetupScreen(
    mode: GameMode,
    onStart: (GameSetup) -> Unit,
    onBack: () -> Unit,
) {
    var state by remember(mode) { mutableStateOf(LocalMatchSetupState(mode)) }
    OnlineStepScaffold(title = stringResource(Res.string.match_setup_title), onBack = onBack) {
        if (state.showPlayerCount) {
            PlayerCountChips(
                options = LOCAL_AI_PLAYER_COUNT_OPTIONS,
                selected = state.playerCount,
                onSelect = { state = state.copy(playerCount = it) },
            )
        }
        if (state.showDifficulty) {
            DifficultyChips(
                selected = state.difficulty,
                onSelect = { state = state.copy(difficulty = it) },
            )
        }
        SeriesFormatChips(
            selected = state.format,
            onSelect = { state = state.copy(format = it) },
        )
        if (state.showTurnTime) {
            TurnTimeChips(
                selected = state.turnTimeSeconds,
                onSelect = { state = state.copy(turnTimeSeconds = it) },
            )
        }
        Button(
            onClick = { onStart(state.toGameSetup()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.match_setup_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Chips de selección de dificultad de la IA, con el mismo estilo que el resto. */
@Composable
private fun DifficultyChips(
    selected: AiDifficulty,
    onSelect: (AiDifficulty) -> Unit,
) {
    OptionChipSection(
        label = stringResource(Res.string.match_setup_difficulty_label),
        icon = Icons.Filled.Star,
    ) {
        DifficultyOption.entries.forEach { option ->
            FilterChip(
                selected = selected == option.difficulty,
                onClick = { onSelect(option.difficulty) },
                label = {
                    Text(
                        text = stringResource(option.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun LocalMatchSetupVersusAiPreview() {
    QuoridorTheme {
        LocalMatchSetupScreen(mode = VERSUS_AI, onStart = {}, onBack = {})
    }
}

@Preview
@Composable
private fun LocalMatchSetupOneVsOnePreview() {
    QuoridorTheme {
        LocalMatchSetupScreen(mode = LOCAL_1V1, onStart = {}, onBack = {})
    }
}

@Preview
@Composable
private fun LocalMatchSetupFourPlayersPreview() {
    QuoridorTheme {
        LocalMatchSetupScreen(mode = FOUR_PLAYERS, onStart = {}, onBack = {})
    }
}
