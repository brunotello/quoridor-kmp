package com.btello.quoridor.presentation.online.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.online.OnlineErrorText
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.online_cancel
import quoridor.app.shared.generated.resources.online_copy_code
import quoridor.app.shared.generated.resources.online_create_match
import quoridor.app.shared.generated.resources.online_player_count
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
    CreateMatchOnlineContent(state = viewModel.uiState, onEvent = viewModel::onEvent, onBack = onBack)
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
                onCancel = { onEvent(CreateMatchOnlineEvent.Cancel) },
            )
        } else {
            PlayerCountSelector(
                selected = state.playerCount,
                onSelect = { onEvent(CreateMatchOnlineEvent.PlayerCountChanged(it)) },
            )
            VisibilityToggle(
                isPublic = state.isPublic,
                onToggle = { onEvent(CreateMatchOnlineEvent.VisibilityChanged(it)) },
            )
            Button(
                onClick = { onEvent(CreateMatchOnlineEvent.CreateMatch) },
                enabled = state.canCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.phase == CreateMatchOnlinePhase.Creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
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
private fun PlayerCountSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.online_player_count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            for (count in MIN_PLAYERS..MAX_PLAYERS step (MAX_PLAYERS - MIN_PLAYERS)) {
                PlayerCountOption(
                    count = count,
                    selected = selected == count,
                    onSelect = { onSelect(count) },
                )
            }
        }
    }
}

@Composable
private fun PlayerCountOption(
    count: Int,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.online_public_label),
                style = MaterialTheme.typography.titleMedium,
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
