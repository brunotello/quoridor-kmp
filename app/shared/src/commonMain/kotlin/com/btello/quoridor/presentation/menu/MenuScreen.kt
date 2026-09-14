package com.btello.quoridor.presentation.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.app_title
import quoridor.app.shared.generated.resources.board_summary
import quoridor.app.shared.generated.resources.config_subtitle
import quoridor.app.shared.generated.resources.players
import quoridor.app.shared.generated.resources.players_option
import quoridor.app.shared.generated.resources.start_game

@Composable
internal fun MenuScreen(
    onNavigateToGame: (GameConfig) -> Unit,
    viewModel: MenuViewModel = viewModel { MenuViewModel() },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is MenuSideEffect.NavigateToGame -> onNavigateToGame(effect.config)
            }
        }
    }

    MenuContent(state = viewModel.uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun MenuContent(
    state: MenuUiState,
    onEvent: (MenuEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.app_title),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(Res.string.config_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.players),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.playerCountOptions.forEach { count ->
                            val isSelected = state.playerCount == count
                            OutlinedButton(
                                onClick = { onEvent(MenuEvent.SelectPlayerCount(count)) },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Text(
                                    text = stringResource(Res.string.players_option, count),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(Res.string.board_summary, state.playerCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }

            Button(
                onClick = { onEvent(MenuEvent.StartGame) },
                modifier = Modifier.padding(top = 28.dp),
            ) {
                Text(
                    text = stringResource(Res.string.start_game),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MenuContentPreview() {
    QuoridorTheme {
        MenuContent(state = MenuUiState(), onEvent = {})
    }
}
