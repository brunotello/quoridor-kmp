package com.btello.quoridor.presentation.online.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.online.OnlineErrorText
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.online.OptionSectionHeader
import com.btello.quoridor.presentation.online.PlayerCountChips
import com.btello.quoridor.presentation.online.SeriesFormatChips
import com.btello.quoridor.presentation.online.TurnTimeChips
import com.btello.quoridor.presentation.online.seriesFormatLabel
import com.btello.quoridor.presentation.online.turnTimeLabel
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.competitive_config_series
import quoridor.app.shared.generated.resources.competitive_config_timer
import quoridor.app.shared.generated.resources.online_cancel
import quoridor.app.shared.generated.resources.online_copy_code
import quoridor.app.shared.generated.resources.online_create_match
import quoridor.app.shared.generated.resources.online_public_description
import quoridor.app.shared.generated.resources.online_public_label
import quoridor.app.shared.generated.resources.online_share_code
import quoridor.app.shared.generated.resources.online_waiting_opponent
import quoridor.app.shared.generated.resources.online_waiting_players

/**
 * Pantalla de creación de una sala online: configura la cantidad de jugadores y
 * la visibilidad, crea la sala y espera a los rivales. Al quedar lista arranca la
 * partida mediante [onStartGame].
 */
@Composable
internal fun CreateMatchOnlineScreen(
    onStartGame: (GameSetup) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateMatchOnlineViewModel = viewModel { CreateMatchOnlineViewModel() },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is OnlineSideEffect.StartGame -> onStartGame(effect.setup)
            }
        }
    }
    CreateMatchOnlineContent(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
private fun CreateMatchOnlineContent(
    state: CreateMatchOnlineUiState,
    onEvent: (CreateMatchOnlineEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_create_match), onBack = onBack) {
        if (state.phase == CreateMatchOnlinePhase.WaitingForOpponent) {
            WaitingSection(
                code = state.hostedCode.orEmpty(),
                joinedCount = state.joinedCount,
                playerCount = state.playerCount,
                format = state.format,
                turnTimeSeconds = state.turnTimeSeconds,
                onCancel = { onEvent(CreateMatchOnlineEvent.Cancel) },
            )
        } else {
            PlayerCountChips(
                options = PLAYER_COUNT_OPTIONS,
                selected = state.playerCount,
                onSelect = { onEvent(CreateMatchOnlineEvent.PlayerCountChanged(it)) },
            )
            SeriesFormatChips(
                selected = state.format,
                onSelect = { onEvent(CreateMatchOnlineEvent.FormatChanged(it)) },
            )
            TurnTimeChips(
                selected = state.turnTimeSeconds,
                onSelect = { onEvent(CreateMatchOnlineEvent.TurnTimeChanged(it)) },
            )
            VisibilityToggle(
                isPublic = state.isPublic,
                onToggle = { onEvent(CreateMatchOnlineEvent.VisibilityChanged(it)) },
            )

            if (state.phase == CreateMatchOnlinePhase.Creating) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                )
            } else {
                Button(
                    onClick = { onEvent(CreateMatchOnlineEvent.CreateMatch) },
                    enabled = state.canCreate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.online_create_match),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        OnlineErrorText(state.error)
    }
}

@Composable
private fun VisibilityToggle(
    isPublic: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionSectionHeader(
                label = stringResource(Res.string.online_public_label),
                icon = Icons.Filled.Public,
            )
            Text(
                text = stringResource(Res.string.online_public_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isPublic,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun WaitingSection(
    code: String,
    joinedCount: Int,
    playerCount: Int,
    format: SeriesFormat,
    turnTimeSeconds: Int?,
    onCancel: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.online_share_code),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = { clipboardManager.setText(AnnotatedString(code)) }) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.online_copy_code),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CompetitiveConfigSummary(format = format, turnTimeSeconds = turnTimeSeconds)
        Text(
            text = stringResource(Res.string.online_waiting_opponent),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.online_waiting_players, joinedCount, playerCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        OutlinedButton(onClick = onCancel) {
            Text(
                text = stringResource(Res.string.online_cancel),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Resumen de la configuración competitiva (rondas y tiempo por turno) de la sala. */
@Composable
internal fun CompetitiveConfigSummary(
    format: SeriesFormat,
    turnTimeSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.competitive_config_series, seriesFormatLabel(format)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                Res.string.competitive_config_timer,
                turnTimeLabel(turnTimeSeconds)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun CreateMatchOnlinePreview() {
    QuoridorTheme {
        CreateMatchOnlineContent(
            state = CreateMatchOnlineUiState(playerName = "Ana"),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CreateMatchOnlineWaitingPreview() {
    QuoridorTheme {
        CreateMatchOnlineContent(
            state = CreateMatchOnlineUiState(
                phase = CreateMatchOnlinePhase.WaitingForOpponent,
                playerName = "Ana",
                hostedCode = "ABC123",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
