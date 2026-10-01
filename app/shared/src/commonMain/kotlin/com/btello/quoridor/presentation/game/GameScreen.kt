package com.btello.quoridor.presentation.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.data.stats.StatisticsProvider
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.domain.online.SeriesFormat.FIRST_TO_3
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.WallPlacement.BoardMetrics
import com.btello.quoridor.presentation.main.labelRes
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.online.formatClock
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.playerColor
import com.btello.quoridor.presentation.theme.safeAreaVerticalPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.ai_player_name
import quoridor.app.shared.generated.resources.ai_thinking
import quoridor.app.shared.generated.resources.difficulty_back
import quoridor.app.shared.generated.resources.feedback_invalid_move
import quoridor.app.shared.generated.resources.feedback_no_legal_walls
import quoridor.app.shared.generated.resources.feedback_no_walls_remaining
import quoridor.app.shared.generated.resources.game_difficulty_label
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.match_starting
import quoridor.app.shared.generated.resources.online_opponent_left
import quoridor.app.shared.generated.resources.online_turn_of
import quoridor.app.shared.generated.resources.online_waiting_opponent
import quoridor.app.shared.generated.resources.online_your_turn
import quoridor.app.shared.generated.resources.player_name
import quoridor.app.shared.generated.resources.player_walls_count
import quoridor.app.shared.generated.resources.wall_drag_hint
import quoridor.app.shared.generated.resources.wall_place_horizontal
import quoridor.app.shared.generated.resources.wall_place_vertical
import kotlin.math.roundToInt

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

    val onBack: () -> Unit = { viewModel.onEvent(GameEvent.LeaveMatch) }
    AppBackHandler { onBack() }

    val state = viewModel.uiState
    val competitive = state.competitive
    if (state.isGameOver && !state.isSeriesOver && competitive != null) {
        LocalSeriesResultScreen(
            competitive = competitive,
            winnerNumber = state.winnerNumber ?: 1,
            result = state.localResult,
            onContinue = { viewModel.onEvent(GameEvent.ContinueSeries) },
        )
    } else if (state.isGameOver) {
        GameResultScreen(
            winnerNumber = state.winnerNumber ?: 1,
            onBackToMenu = { viewModel.onEvent(GameEvent.NewGame) },
            result = state.localResult,
        )
    } else {
        GameContent(
            state = state,
            onEvent = viewModel::onEvent,
            onBack = onBack,
        )
    }
}

@Composable
internal fun GameContent(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
    onBack: () -> Unit,
) {
    val gameState = state.gameState
    val activePlayer = gameState.players.first { it.id == gameState.turn.playerId }
    val topPlayers = gameState.players.filter { it.id.value % 2 == 0 }
    val bottomPlayers = gameState.players.filter { it.id.value % 2 == 1 }
    var boardMetrics by remember { mutableStateOf<BoardMetrics?>(null) }
    var wallDrag by remember { mutableStateOf<WallDrag?>(null) }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    val wallLiftPx = with(LocalDensity.current) { WallDragLift.toPx() }
    LaunchedEffect(state.canPlaceWall) {
        if (!state.canPlaceWall) wallDrag = null
    }
    val previewWall = wallDrag?.let { drag ->
        boardMetrics?.let { WallPlacement.wallAt(it, drag.pointer.x, drag.pointer.y, drag.orientation) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeAreaVerticalPadding()
                .onGloballyPositioned { containerOrigin = it.positionInRoot() },
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.competitive?.let { competitive ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompetitivePanel(
                        competitive = competitive,
                        playerNames = state.playerNames,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            // Espacio flexible igual a la suma de los dos inferiores (que rodean los
            // controles de muro): centra el tablero y los controles quedan centrados
            // en el espacio que sobra debajo.
            Spacer(modifier = Modifier.weight(2f))
            state.competitive?.let { competitive ->
                // El indicador ocupa siempre su altura real: así no se recorta el
                // temporizador y el tablero no se desplaza.
                TurnBannerWithTimer(
                    banner = state.turnBanner,
                    localPlayerId = state.localPlayerId,
                    turnRemainingMillis = competitive.turnRemainingMillis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PlayerPanelRow(
                players = topPlayers,
                activeId = activePlayer.id,
                aiPlayers = state.aiPlayers,
                isAiThinking = state.isAiThinking,
                playerNames = state.playerNames,
            )

            val feedback = state.feedback
            if (feedback != null && state.matchIntro == null) {
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
                    onActivePawnClick = { onEvent(GameEvent.ActivePawnClick) },
                    onCellClick = { onEvent(GameEvent.CellClick(it)) },
                    previewWall = previewWall,
                    highlightedWalls = state.highlightedWalls(wallDrag?.orientation),
                    onMetricsChanged = { boardMetrics = it },
                )
                state.matchIntro?.let { intro ->
                    MatchIntroOverlay(intro = intro)
                }
            }

            PlayerPanelRow(
                players = bottomPlayers,
                activeId = activePlayer.id,
                aiPlayers = state.aiPlayers,
                isAiThinking = state.isAiThinking,
                playerNames = state.playerNames,
            )

            Spacer(modifier = Modifier.weight(1f))
            WallControls(
                enabled = state.canPlaceWall,
                onDrag = { orientation, finger ->
                    wallDrag = WallDrag(orientation, finger - Offset(0f, wallLiftPx))
                },
                onRelease = {
                    val drag = wallDrag
                    val metrics = boardMetrics
                    if (drag != null && metrics != null) {
                        WallPlacement.wallAt(metrics, drag.pointer.x, drag.pointer.y, drag.orientation)
                            ?.let { onEvent(GameEvent.WallDrop(it)) }
                    }
                    wallDrag = null
                },
                onCancel = { wallDrag = null },
            )
            Spacer(modifier = Modifier.weight(1f))
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

            state.difficulty?.let { difficulty ->
                Text(
                    text = stringResource(Res.string.game_difficulty_label, stringResource(difficulty.labelRes())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                )
            }

            if (state.competitive == null) {
                state.turnBanner?.let { banner ->
                    Text(
                        text = turnBannerText(banner),
                        style = MaterialTheme.typography.headlineSmall,
                        color = playerColor(turnBannerPlayerId(banner, state.localPlayerId)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(start = 16.dp, end = 16.dp, top = 56.dp),
                    )
                }
            }

            // Fuera del tablero el muro sigue al dedo; sobre el tablero se muestra encajado en su ranura.
            val drag = wallDrag
            val metrics = boardMetrics
            if (drag != null && metrics != null && previewWall == null) {
                FloatingWall(
                    orientation = drag.orientation,
                    center = drag.pointer - containerOrigin,
                    metrics = metrics,
                )
            }
        }
    }
}

/** Distancia a la que el muro arrastrado se dibuja por encima del dedo, para que no lo tape. */
private val WallDragLift = 56.dp

/**
 * Arrastre de un muro en curso: [orientation] elegida y [pointer], el punto (en
 * coordenadas de la raíz) donde se dibuja y encaja el muro, ya elevado
 * [WallDragLift] por encima del dedo.
 */
private data class WallDrag(val orientation: WallOrientation, val pointer: Offset)

/** Muro "flotante" (gris claro) centrado en [center], con el tamaño real de un muro del tablero. */
@Composable
private fun FloatingWall(
    orientation: WallOrientation,
    center: Offset,
    metrics: BoardMetrics,
) {
    val lengthPx = WallPlacement.wallLengthPx(metrics)
    val thicknessPx = metrics.gapPx
    val (widthPx, heightPx) = when (orientation) {
        WallOrientation.HORIZONTAL -> lengthPx to thicknessPx
        WallOrientation.VERTICAL -> thicknessPx to lengthPx
    }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (center.x - widthPx / 2f).roundToInt(),
                    y = (center.y - heightPx / 2f).roundToInt(),
                )
            }
            .size(
                width = with(density) { widthPx.toDp() },
                height = with(density) { heightPx.toDp() },
            )
            .clip(RoundedCornerShape(2.dp))
            .background(QuoridorTheme.boardColors.wallPreview),
    )
}

/**
 * Botones para colocar un muro horizontal o vertical, con una indicación arriba.
 * Al presionar un botón el muro aparece bajo el dedo y lo sigue; al soltar sobre
 * el tablero se coloca. Si se suelta fuera, no se coloca.
 */
@Composable
private fun WallControls(
    enabled: Boolean,
    onDrag: (WallOrientation, Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(WallHintIconSize),
            )
            Text(
                text = stringResource(Res.string.wall_drag_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WallDragButton(
                orientation = WallOrientation.HORIZONTAL,
                label = stringResource(Res.string.wall_place_horizontal),
                enabled = enabled,
                onDrag = onDrag,
                onRelease = onRelease,
                onCancel = onCancel,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            WallDragButton(
                orientation = WallOrientation.VERTICAL,
                label = stringResource(Res.string.wall_place_vertical),
                enabled = enabled,
                onDrag = onDrag,
                onRelease = onRelease,
                onCancel = onCancel,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

private val WallHintIconSize = 16.dp
private const val WallButtonDisabledAlpha = 0.4f
private val WallGlyphThickness = 6.dp
private val WallGlyphLength = 24.dp

@Composable
private fun WallDragButton(
    orientation: WallOrientation,
    label: String,
    enabled: Boolean,
    onDrag: (WallOrientation, Offset) -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnCancel by rememberUpdatedState(onCancel)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .alpha(if (enabled) 1f else WallButtonDisabledAlpha)
            .onGloballyPositioned { originInRoot = it.positionInRoot() }
            .then(
                if (enabled) {
                    // Sin umbral de arrastre: el muro aparece bajo el dedo apenas se presiona.
                    Modifier.pointerInput(orientation) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            currentOnDrag(orientation, originInRoot + down.position)
                            var released = false
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    released = true
                                    break
                                }
                                change.consume()
                                currentOnDrag(orientation, originInRoot + change.position)
                            }
                            if (released) currentOnRelease() else currentOnCancel()
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (glyphWidth, glyphHeight) = when (orientation) {
                WallOrientation.HORIZONTAL -> WallGlyphLength to WallGlyphThickness
                WallOrientation.VERTICAL -> WallGlyphThickness to WallGlyphLength
            }
            // Caja cuadrada fija para que ambos botones tengan el mismo tamaño.
            Box(
                modifier = Modifier.size(WallGlyphLength),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = glyphWidth, height = glyphHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(QuoridorTheme.boardColors.wallPlaced),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun feedbackText(feedback: GameFeedback): String = when (feedback) {
    GameFeedback.NoWallsRemaining -> stringResource(Res.string.feedback_no_walls_remaining)
    GameFeedback.NoLegalWalls -> stringResource(Res.string.feedback_no_legal_walls)
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

/** Umbral (ms) a partir del cual el temporizador del turno se resalta como urgente. */
private const val LOW_TURN_TIME_MILLIS = 10_000L

/** `true` cuando al turno le quedan [LOW_TURN_TIME_MILLIS] o menos. */
internal fun isTurnTimeLow(remainingMillis: Long): Boolean = remainingMillis <= LOW_TURN_TIME_MILLIS

/**
 * Indicador de turno online con el temporizador del turno debajo, en un estilo
 * más chico. Ambas líneas reservan siempre su espacio (aunque no haya indicador),
 * de modo que el resto de la pantalla no se desplaza al aparecer o cambiar.
 * [turnRemainingMillis] es `null` cuando la partida no tiene temporizador.
 */
@Composable
private fun TurnBannerWithTimer(
    banner: TurnBanner?,
    localPlayerId: PlayerId?,
    turnRemainingMillis: Long?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = banner?.let { turnBannerText(it) }.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = banner?.let { playerColor(turnBannerPlayerId(it, localPlayerId)) }
                ?: MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        turnRemainingMillis?.let { millis ->
            Text(
                text = formatClock(millis),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isTurnTimeLow(millis)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (banner != null) 1f else 0f),
            )
        }
    }
}

/** Opacidad del velo que atenúa el tablero mientras se muestra la introducción. */
private const val MatchIntroScrimAlpha = 0.85f

/**
 * Velo centrado sobre el tablero que muestra la introducción de la partida
 * (espera de jugadores o cuenta atrás) y lo deshabilita visualmente hasta que
 * empieza el juego.
 */
@Composable
private fun BoxScope.MatchIntroOverlay(intro: MatchIntro) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = MatchIntroScrimAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        val isCountdownNumber = intro is MatchIntro.Countdown && intro.value > 0
        Text(
            text = matchIntroText(intro),
            style = if (isCountdownNumber) {
                MaterialTheme.typography.displayLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = if (intro is MatchIntro.WaitingForPlayers) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

/** Texto de la introducción de la partida resuelto desde `strings.xml`. */
@Composable
private fun matchIntroText(intro: MatchIntro): String = when (intro) {
    MatchIntro.WaitingForPlayers -> stringResource(Res.string.online_waiting_opponent)
    is MatchIntro.Countdown ->
        if (intro.value > 0) intro.value.toString() else stringResource(Res.string.match_starting)
}

@Composable
private fun PlayerPanelRow(
    players: List<Player>,
    activeId: PlayerId,
    aiPlayers: Set<PlayerId>,
    isAiThinking: Boolean,
    playerNames: List<String>,
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
    modifier: Modifier = Modifier,
) {
    Surface(
        contentColor = QuoridorTheme.boardColors.pawnLabel,
        shape = RoundedCornerShape(4 .dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
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
                    color = playerColor(player.id.value)
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
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(player.wallsRemaining) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = WallReserveHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(playerColor(player.id.value)),
                    )
                }
                Text(
                    text = stringResource(Res.string.player_walls_count, player.wallsRemaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = playerColor(player.id.value),
                )
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
private fun WallControlsPreview() {
    QuoridorTheme {
        WallControls(
            enabled = true,
            onDrag = { _, _ -> },
            onRelease = {},
            onCancel = {},
        )
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
private fun GameContentCompetitiveTimerPreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(
                gameState = QuoridorRules.startGame(GameConfig(playerCount = 2)),
                playerNames = listOf("Ana", "Beto"),
                localPlayerId = PlayerId(0),
                turnBanner = TurnBanner.YourTurn,
                competitive = CompetitiveUi(
                    format = FIRST_TO_3,
                    wins = listOf(1, 0),
                    gamesToWin = 3,
                    turnRemainingMillis = 27_000L,
                    localPlayerId = PlayerId(0),
                ),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GameContentVersusAiPreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(
                gameState = QuoridorRules.startGame(GameConfig(playerCount = 2)),
                aiPlayers = setOf(PlayerId(1)),
                difficulty = AiDifficulty.HARD,
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
private fun TurnBannerWithTimerPreview() {
    QuoridorTheme {
        TurnBannerWithTimer(
            banner = TurnBanner.PlayerTurn(playerNumber = 2, playerName = "Beto"),
            localPlayerId = PlayerId(0),
            turnRemainingMillis = 8_000L,
        )
    }
}

@Preview
@Composable
private fun GameContentWaitingPreview() {
    QuoridorTheme {
        GameContent(
            state = GameUiState(
                gameState = QuoridorRules.startGame(GameConfig(playerCount = 2)),
                playerNames = listOf("Ana"),
                localPlayerId = PlayerId(0),
                matchIntro = MatchIntro.WaitingForPlayers,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun MatchIntroWaitingOverlayPreview() {
    QuoridorTheme {
        Box(modifier = Modifier.size(240.dp)) {
            MatchIntroOverlay(intro = MatchIntro.WaitingForPlayers)
        }
    }
}

@Preview
@Composable
private fun MatchIntroCountdownOverlayPreview() {
    QuoridorTheme {
        Box(modifier = Modifier.size(240.dp)) {
            MatchIntroOverlay(intro = MatchIntro.Countdown(value = 3))
        }
    }
}

@Preview
@Composable
private fun MatchIntroStartingOverlayPreview() {
    QuoridorTheme {
        Box(modifier = Modifier.size(240.dp)) {
            MatchIntroOverlay(intro = MatchIntro.Countdown(value = 0))
        }
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
        )
    }
}
