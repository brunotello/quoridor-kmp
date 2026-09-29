package com.btello.quoridor.presentation.online.join

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.online.OnlineError
import com.btello.quoridor.presentation.online.OnlineErrorText
import com.btello.quoridor.presentation.online.OnlineSideEffect
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.online_join_hint
import quoridor.app.shared.generated.resources.online_join_match

/**
 * Pantalla de unión a una sala online por código. Al lograr la unión arranca la
 * partida mediante [onStartGame]; si se agota el tiempo muestra un error.
 */
@Composable
internal fun JoinMatchOnlineScreen(
    onStartGame: (GameSetup) -> Unit,
    onBack: () -> Unit,
    viewModel: JoinMatchOnlineViewModel = viewModel { JoinMatchOnlineViewModel() },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is OnlineSideEffect.StartGame -> onStartGame(effect.setup)
            }
        }
    }
    JoinMatchOnlineContent(state = viewModel.uiState, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
private fun JoinMatchOnlineContent(
    state: JoinMatchOnlineUiState,
    onEvent: (JoinMatchOnlineEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_join_match), onBack = onBack) {
        OutlinedTextField(
            value = state.joinCode,
            onValueChange = { onEvent(JoinMatchOnlineEvent.JoinCodeChanged(it)) },
            singleLine = true,
            label = {
                Text(
                    text = stringResource(Res.string.online_join_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onEvent(JoinMatchOnlineEvent.JoinMatch) },
            enabled = state.canJoinByCode,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.phase == JoinMatchOnlinePhase.Joining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(Res.string.online_join_match),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        OnlineErrorText(state.error)
    }
}

@Preview
@Composable
private fun JoinMatchOnlinePreview() {
    QuoridorTheme {
        JoinMatchOnlineContent(
            state = JoinMatchOnlineUiState(playerName = "Ana", joinCode = "ABC123"),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun JoinMatchOnlineTimeoutPreview() {
    QuoridorTheme {
        JoinMatchOnlineContent(
            state = JoinMatchOnlineUiState(
                playerName = "Ana",
                joinCode = "ABC123",
                error = OnlineError.JoinTimeout,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
