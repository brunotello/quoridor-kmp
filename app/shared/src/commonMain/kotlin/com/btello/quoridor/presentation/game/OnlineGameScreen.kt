package com.btello.quoridor.presentation.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.game_leave_confirm
import quoridor.app.shared.generated.resources.game_leave_dismiss
import quoridor.app.shared.generated.resources.game_leave_message_continue
import quoridor.app.shared.generated.resources.game_leave_message_lose
import quoridor.app.shared.generated.resources.game_leave_title

/**
 * Pantalla de una partida online. Reutiliza [GameContent] y [GameResultScreen]
 * del modo local y añade únicamente lo propio del online: confirmación al salir
 * (que puede suponer perder la partida) y la opción de continuar hacia el menú al
 * terminar. La lógica vive en [OnlineGameViewModel].
 */
@Composable
internal fun OnlineGameScreen(
    setup: GameSetup,
    sessionKey: Int,
    onNavigateToMenu: () -> Unit,
    viewModel: OnlineGameViewModel = viewModel(key = "online-game-$sessionKey") {
        OnlineGameViewModel(
            setup,
            onlineRepository = OnlinePlatform.repositoryOrNull(),
        )
    },
) {
    var confirmingLeave by remember { mutableStateOf(false) }
    val onBackRequested: () -> Unit = { confirmingLeave = true }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                GameSideEffect.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    AppBackHandler { onBackRequested() }

    val state = viewModel.uiState
    val competitive = state.competitive
    if (state.isGameOver && !state.isSeriesOver && competitive != null) {
        SeriesResultScreen(
            competitive = competitive,
            result = state.localResult,
            onContinue = { viewModel.onEvent(GameEvent.ContinueSeries) },
        )
    } else if (state.isGameOver) {
        GameResultScreen(
            winnerNumber = state.winnerNumber ?: 1,
            onBackToMenu = { viewModel.onEvent(GameEvent.NewGame) },
            result = state.localResult,
            isAbandoned = state.isAbandoned,
            onContinue = { viewModel.onEvent(GameEvent.NewGame) },
        )
    } else {
        GameContent(
            state = state,
            onEvent = viewModel::onEvent,
            onBack = onBackRequested,
        )
    }

    if (confirmingLeave) {
        LeaveMatchConfirmDialog(
            losesMatch = state.gameState.players.size <= 2,
            onConfirm = {
                confirmingLeave = false
                viewModel.onEvent(GameEvent.LeaveMatch)
            },
            onDismiss = { confirmingLeave = false },
        )
    }
}

@Composable
private fun LeaveMatchConfirmDialog(
    losesMatch: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.game_leave_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = if (losesMatch) {
                    stringResource(Res.string.game_leave_message_lose)
                } else {
                    stringResource(Res.string.game_leave_message_continue)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.game_leave_confirm),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.game_leave_dismiss),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Preview
@Composable
private fun LeaveMatchConfirmDialogPreview() {
    QuoridorTheme {
        LeaveMatchConfirmDialog(losesMatch = true, onConfirm = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun LeaveMatchContinueDialogPreview() {
    QuoridorTheme {
        LeaveMatchConfirmDialog(losesMatch = false, onConfirm = {}, onDismiss = {})
    }
}
