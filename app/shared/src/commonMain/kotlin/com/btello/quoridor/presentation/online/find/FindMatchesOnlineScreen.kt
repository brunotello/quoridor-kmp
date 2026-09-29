package com.btello.quoridor.presentation.online.find

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.online.OnlineErrorText
import com.btello.quoridor.presentation.online.OnlineOpenMatch
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.online_browse_empty
import quoridor.app.shared.generated.resources.online_browse_match
import quoridor.app.shared.generated.resources.online_browse_refresh
import quoridor.app.shared.generated.resources.online_join_match
import quoridor.app.shared.generated.resources.online_open_match_host
import quoridor.app.shared.generated.resources.online_open_match_players

/**
 * Pantalla de búsqueda de salas públicas. Lista las salas disponibles en tiempo
 * real y permite unirse a una sin código; al lograrlo arranca la partida
 * mediante [onStartGame].
 */
@Composable
internal fun FindMatchesOnlineScreen(
    onStartGame: (GameSetup) -> Unit,
    onBack: () -> Unit,
    viewModel: FindMatchesOnlineViewModel = viewModel { FindMatchesOnlineViewModel() },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is OnlineSideEffect.StartGame -> onStartGame(effect.setup)
            }
        }
    }
    FindMatchesOnlineContent(state = viewModel.uiState, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
private fun FindMatchesOnlineContent(
    state: FindMatchesOnlineUiState,
    onEvent: (FindMatchesOnlineEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(
        title = stringResource(Res.string.online_browse_match),
        onBack = onBack,
        action = {
            IconButton(
                onClick = { onEvent(FindMatchesOnlineEvent.Refresh) },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(Res.string.online_browse_refresh),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    ) {
        if (state.openMatches.isEmpty()) {
            if (state.phase == FindMatchesOnlinePhase.Joining) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = stringResource(Res.string.online_browse_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            for (match in state.openMatches) {
                OpenMatchCard(
                    match = match,
                    enabled = state.canJoin,
                    joining = state.phase == FindMatchesOnlinePhase.Joining,
                    onJoin = { onEvent(FindMatchesOnlineEvent.JoinPublicMatch(match.id)) },
                )
            }
        }
        OnlineErrorText(state.error)
    }
}

@Composable
private fun OpenMatchCard(
    match: OnlineOpenMatch,
    enabled: Boolean,
    joining: Boolean,
    onJoin: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(Res.string.online_open_match_host, match.hostName),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    Res.string.online_open_match_players,
                    match.joinedCount,
                    match.playerCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (joining) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Button(onClick = onJoin, enabled = enabled) {
                Text(
                    text = stringResource(Res.string.online_join_match),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FindMatchesOnlinePreview() {
    QuoridorTheme {
        FindMatchesOnlineContent(
            state = FindMatchesOnlineUiState(
                playerName = "Ana",
                openMatches = listOf(
                    OnlineOpenMatch(MatchId("ABC123"), hostName = "Beto", joinedCount = 1, playerCount = 2),
                    OnlineOpenMatch(MatchId("XYZ789"), hostName = "Caro", joinedCount = 2, playerCount = 4),
                ),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun FindMatchesOnlineEmptyPreview() {
    QuoridorTheme {
        FindMatchesOnlineContent(
            state = FindMatchesOnlineUiState(playerName = "Ana"),
            onEvent = {},
            onBack = {},
        )
    }
}
