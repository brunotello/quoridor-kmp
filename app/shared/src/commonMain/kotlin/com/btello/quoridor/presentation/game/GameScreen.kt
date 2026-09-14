package com.btello.quoridor.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.playerColor
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.feedback_invalid_move
import quoridor.app.shared.generated.resources.feedback_invalid_wall
import quoridor.app.shared.generated.resources.feedback_no_legal_walls
import quoridor.app.shared.generated.resources.feedback_no_walls_remaining
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.player_name

@Composable
internal fun GameScreen(
    config: GameConfig,
    sessionKey: Int,
    onNavigateToMenu: () -> Unit,
    viewModel: GameViewModel = viewModel(key = "game-$sessionKey") { GameViewModel(config) },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                GameSideEffect.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    val state = viewModel.uiState
    if (state.isGameOver) {
        GameResultScreen(
            winnerNumber = state.winnerNumber ?: 1,
            onNewGame = { viewModel.onEvent(GameEvent.NewGame) },
        )
    } else {
        GameContent(state = state, onEvent = viewModel::onEvent)
    }
}

@Composable
private fun GameContent(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
) {
    val gameState = state.gameState
    val activePlayer = gameState.players.first { it.id == gameState.turn.playerId }
    val topPlayer = gameState.players.firstOrNull { it.id.value == 0 }
    val bottomPlayer = gameState.players.firstOrNull { it.id.value == 1 }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            if (topPlayer != null) {
                PlayerPanel(
                    player = topPlayer,
                    isActive = topPlayer.id == activePlayer.id,
                    onClick = { onEvent(GameEvent.WallReserveClick) },
                )
            }

            val feedback = state.feedback
            if (feedback != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = feedbackText(feedback),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoardView(
                    state = gameState,
                    legalTargets = state.legalTargets,
                    legalWalls = state.legalWalls,
                    onActivePawnClick = { onEvent(GameEvent.ActivePawnClick) },
                    onCellClick = { onEvent(GameEvent.CellClick(it)) },
                    onWallClick = { onEvent(GameEvent.WallClick(it)) },
                )
            }

            if (bottomPlayer != null) {
                PlayerPanel(
                    player = bottomPlayer,
                    isActive = bottomPlayer.id == activePlayer.id,
                    onClick = { onEvent(GameEvent.WallReserveClick) },
                )
            }
        }
    }
}

@Composable
private fun feedbackText(feedback: GameFeedback): String = when (feedback) {
    GameFeedback.NoWallsRemaining -> stringResource(Res.string.feedback_no_walls_remaining)
    GameFeedback.NoLegalWalls -> stringResource(Res.string.feedback_no_legal_walls)
    GameFeedback.InvalidWall -> stringResource(Res.string.feedback_invalid_wall)
    GameFeedback.InvalidMove -> stringResource(Res.string.feedback_invalid_move)
    GameFeedback.GameOver -> stringResource(Res.string.game_over)
    is GameFeedback.DomainMessage -> feedback.text
}

@Composable
private fun PlayerPanel(
    player: Player,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = playerColor(player.id.value),
        contentColor = QuoridorTheme.boardColors.pawnLabel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.player_name, player.id.value + 1),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(if (isActive) Modifier.clickable { onClick() } else Modifier)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(player.wallsRemaining) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(QuoridorTheme.boardColors.wallReserve),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GameContentPreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(gameState = QuoridorRules.startGame(GameConfig(playerCount = 2))),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun PlayerPanelPreview() {
    QuoridorTheme {
        PlayerPanel(
            player = QuoridorRules.startGame(GameConfig(playerCount = 2)).players.first(),
            isActive = true,
            onClick = {},
        )
    }
}
