package com.btello.quoridor.presentation.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.data.online.OnlinePlatform
import com.btello.quoridor.data.stats.StatisticsProvider
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.playerColor
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.ai_player_name
import quoridor.app.shared.generated.resources.ai_thinking
import quoridor.app.shared.generated.resources.difficulty_back
import quoridor.app.shared.generated.resources.feedback_invalid_move
import quoridor.app.shared.generated.resources.feedback_invalid_wall
import quoridor.app.shared.generated.resources.feedback_no_legal_walls
import quoridor.app.shared.generated.resources.feedback_no_walls_remaining
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.online_opponent_left
import quoridor.app.shared.generated.resources.online_turn_of
import quoridor.app.shared.generated.resources.online_waiting_opponent
import quoridor.app.shared.generated.resources.online_your_turn
import quoridor.app.shared.generated.resources.player_name

private val WallReserveHeight = 24.dp
private const val SHIMMER_DURATION_MILLIS = 1400
private const val SHIMMER_BAND_FRACTION = 0.35f
private const val SHIMMER_ALPHA = 0.25f

@Composable
internal fun GameScreen(
    setup: GameSetup,
    sessionKey: Int,
    onNavigateToMenu: () -> Unit,
    viewModel: GameViewModel = viewModel(key = "game-$sessionKey") {
        GameViewModel(
            setup,
            statisticsRepository = StatisticsProvider.repository,
            onlineRepository = if (setup.online != null) OnlinePlatform.repositoryOrNull() else null,
        )
    },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                GameSideEffect.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    AppBackHandler { onNavigateToMenu() }

    val state = viewModel.uiState
    if (state.isGameOver) {
        GameResultScreen(
            winnerNumber = state.winnerNumber ?: 1,
            onNewGame = { viewModel.onEvent(GameEvent.NewGame) },
            isAbandoned = state.isAbandoned,
        )
    } else {
        GameContent(
            state = state,
            onEvent = viewModel::onEvent,
            onBack = onNavigateToMenu,
        )
    }
}

@Composable
private fun GameContent(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
    onBack: () -> Unit,
) {
    val gameState = state.gameState
    val activePlayer = gameState.players.first { it.id == gameState.turn.playerId }
    val topPlayers = gameState.players.filter { it.id.value % 2 == 0 }
    val bottomPlayers = gameState.players.filter { it.id.value % 2 == 1 }
    var isBoardZoomed by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().safeAreaTopPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            AnimatedVisibility(visible = !isBoardZoomed) {
                PlayerPanelRow(
                    players = topPlayers,
                    activeId = activePlayer.id,
                    aiPlayers = state.aiPlayers,
                    isAiThinking = state.isAiThinking,
                    playerNames = state.playerNames,
                    onWallReserveClick = { onEvent(GameEvent.WallReserveClick) },
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
                    onZoomedChange = { isBoardZoomed = it },
                )
            }

            AnimatedVisibility(visible = !isBoardZoomed) {
                PlayerPanelRow(
                    players = bottomPlayers,
                    activeId = activePlayer.id,
                    aiPlayers = state.aiPlayers,
                    isAiThinking = state.isAiThinking,
                    playerNames = state.playerNames,
                    onWallReserveClick = { onEvent(GameEvent.WallReserveClick) },
                )
            }
        }
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.difficulty_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            state.turnBanner?.let { banner ->
                Text(
                    text = turnBannerText(banner),
                    style = MaterialTheme.typography.headlineSmall,
                    color = playerColor(turnBannerPlayerId(banner, state.localPlayerId)),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 56.dp),
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
    GameFeedback.WaitingOpponent -> stringResource(Res.string.online_waiting_opponent)
    GameFeedback.OpponentLeft -> stringResource(Res.string.online_opponent_left)
    is GameFeedback.DomainMessage -> feedback.text
}

/** Texto del indicador de turno online mostrado sobre el tablero. */
@Composable
private fun turnBannerText(banner: TurnBanner): String = when (banner) {
    TurnBanner.YourTurn -> stringResource(Res.string.online_your_turn)
    is TurnBanner.PlayerTurn -> stringResource(
        Res.string.online_turn_of,
        banner.playerName ?: stringResource(Res.string.player_name, banner.playerNumber),
    )
}

/**
 * [PlayerId.value] (base 0) del jugador cuyo turno describe el [banner], usado
 * para teñir el texto con su color.
 */
internal fun turnBannerPlayerId(banner: TurnBanner, localPlayerId: PlayerId?): Int = when (banner) {
    TurnBanner.YourTurn -> localPlayerId?.value ?: 0
    is TurnBanner.PlayerTurn -> banner.playerNumber - 1
}

@Composable
private fun PlayerPanelRow(
    players: List<Player>,
    activeId: PlayerId,
    aiPlayers: Set<PlayerId>,
    isAiThinking: Boolean,
    playerNames: List<String>,
    onWallReserveClick: () -> Unit,
) {
    if (players.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        players.forEach { player ->
            val isAi = player.id in aiPlayers
            val isActive = player.id == activeId
            PlayerPanel(
                player = player,
                isActive = isActive,
                isAi = isAi,
                isThinking = isAi && isActive && isAiThinking,
                playerName = playerNames.getOrNull(player.id.value)?.takeIf { it.isNotBlank() },
                onClick = onWallReserveClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerPanel(
    player: Player,
    isActive: Boolean,
    isAi: Boolean,
    isThinking: Boolean,
    playerName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = playerColor(player.id.value),
        contentColor = QuoridorTheme.boardColors.pawnLabel,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .turnShimmer(active = isActive),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        playerName != null -> playerName
                        isAi -> stringResource(Res.string.ai_player_name)
                        else -> stringResource(Res.string.player_name, player.id.value + 1)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = QuoridorTheme.boardColors.pawnLabel,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .heightIn(min = WallReserveHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .then(if (isActive) Modifier.clickable { onClick() } else Modifier)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(player.wallsRemaining) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = WallReserveHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(QuoridorTheme.boardColors.wallReserve),
                    )
                }
            }
        }
    }
}

/**
 * Barrido de luz (shimmer) que recorre el banner mientras es el turno del
 * jugador. El color de realce sale del tema (no se hardcodea) y la animación se
 * detiene cuando [active] es `false`.
 */
@Composable
private fun Modifier.turnShimmer(active: Boolean): Modifier {
    if (!active) return this
    val highlight = QuoridorTheme.boardColors.pawnLabel.copy(alpha = SHIMMER_ALPHA)
    val transition = rememberInfiniteTransition(label = "turnShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "turnShimmerProgress",
    )
    return drawWithCache {
        val bandWidth = size.width * SHIMMER_BAND_FRACTION
        val startX = -bandWidth + (size.width + bandWidth) * progress
        val brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, highlight, Color.Transparent),
            start = Offset(startX, 0f),
            end = Offset(startX + bandWidth, size.height),
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush)
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
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GameContentFourPlayersPreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(
                gameState = QuoridorRules.startGame(GameConfig(playerCount = 4)),
                aiPlayers = setOf(PlayerId(2), PlayerId(3)),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GameContentOnlinePreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(
                gameState = QuoridorRules.startGame(GameConfig(playerCount = 2)),
                playerNames = listOf("Ana", "Beto"),
                localPlayerId = PlayerId(0),
                turnBanner = TurnBanner.YourTurn,
            ),
            onEvent = {},
            onBack = {},
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
            isThinking = false,
            playerName = null,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun NamedPlayerPanelPreview() {
    QuoridorTheme {
        PlayerPanel(
            player = QuoridorRules.startGame(GameConfig(playerCount = 2)).players.first(),
            isActive = true,
            isAi = false,
            isThinking = false,
            playerName = "Ana",
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun AiThinkingPanelPreview() {
    QuoridorTheme {
        PlayerPanel(
            player = QuoridorRules.startGame(GameConfig(playerCount = 2)).players.first(),
            isActive = true,
            isAi = true,
            isThinking = true,
            playerName = null,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun NoWallsPanelPreview() {
    QuoridorTheme {
        PlayerPanel(
            player = QuoridorRules.startGame(GameConfig(playerCount = 2)).players.first().copy(wallsRemaining = 0),
            isActive = false,
            isAi = false,
            isThinking = false,
            playerName = null,
            onClick = {},
        )
    }
}
