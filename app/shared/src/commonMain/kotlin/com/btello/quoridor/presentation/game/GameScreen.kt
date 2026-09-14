package com.btello.quoridor.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import quoridor.app.shared.generated.resources.ai_player_name
import quoridor.app.shared.generated.resources.ai_thinking
import quoridor.app.shared.generated.resources.feedback_invalid_move
import quoridor.app.shared.generated.resources.feedback_invalid_wall
import quoridor.app.shared.generated.resources.feedback_no_legal_walls
import quoridor.app.shared.generated.resources.feedback_no_walls_remaining
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.player_name

@Composable
internal fun GameScreen(
    setup: GameSetup,
    sessionKey: Int,
    onNavigateToMenu: () -> Unit,
    viewModel: GameViewModel = viewModel(key = "game-$sessionKey") { GameViewModel(setup) },
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
                val topIsAi = topPlayer.id in state.aiPlayers
                PlayerPanel(
                    player = topPlayer,
                    isActive = topPlayer.id == activePlayer.id,
                    isAi = topIsAi,
                    onClick = { onEvent(GameEvent.WallReserveClick) },
                )
                if (topIsAi) {
                    AiThinkingSlot(visible = state.isAiThinking)
                }
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
                val bottomIsAi = bottomPlayer.id in state.aiPlayers
                PlayerPanel(
                    player = bottomPlayer,
                    isActive = bottomPlayer.id == activePlayer.id,
                    isAi = bottomIsAi,
                    onClick = { onEvent(GameEvent.WallReserveClick) },
                )
                if (bottomIsAi) {
                    AiThinkingSlot(visible = state.isAiThinking)
                }
            }
        }
    }
}

@Composable
private fun AiThinkingSlot(visible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (visible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.ai_thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
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
    GameFeedback.AiThinking -> stringResource(Res.string.ai_thinking)
    is GameFeedback.DomainMessage -> feedback.text
}

@Composable
private fun PlayerPanel(
    player: Player,
    isActive: Boolean,
    isAi: Boolean,
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
                text = if (isAi) {
                    stringResource(Res.string.ai_player_name)
                } else {
                    stringResource(Res.string.player_name, player.id.value + 1)
                },
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
            isAi = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun AiThinkingSlotPreview() {
    QuoridorTheme {
        AiThinkingSlot(visible = true)
    }
}
