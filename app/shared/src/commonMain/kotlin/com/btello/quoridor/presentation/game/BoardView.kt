package com.btello.quoridor.presentation.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.GoalSide.BOTTOM
import com.btello.quoridor.domain.model.GoalSide.LEFT
import com.btello.quoridor.domain.model.GoalSide.RIGHT
import com.btello.quoridor.domain.model.GoalSide.TOP
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.domain.rules.QuoridorRules
import com.btello.quoridor.presentation.game.WallPlacement.BoardMetrics
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.playerColor
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.pawn_label

private const val LEGAL_WALL_ALPHA = 0.35f

@Composable
internal fun BoardView(
    state: GameState,
    legalTargets: Set<Cell>,
    onActivePawnClick: () -> Unit,
    onCellClick: (Cell) -> Unit,
    previewWall: Wall? = null,
    highlightedWalls: Set<Wall> = emptySet(),
    onMetricsChanged: (BoardMetrics) -> Unit = {},
) {
    val grid = BoardGrid(state, legalTargets, highlightedWalls)
    val gap = 8.dp
    val boardPadding = 4.dp
    val boardSize = grid.boardSize
    val boardColors = QuoridorTheme.boardColors
    val activePlayerColor = playerColor(state.players.first { it.id == state.turn.playerId }.id.value)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(boardColors.background)
            .padding(boardPadding),
    ) {
        val contentSize = maxWidth
        val cellSize = (contentSize - gap * (boardSize - 1)) / boardSize
        val step = cellSize + gap

        val gapPx = with(LocalDensity.current) { gap.toPx() }
        val onMetrics = rememberUpdatedState(onMetricsChanged)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    onMetrics.value(
                        BoardMetrics(
                            left = bounds.left,
                            top = bounds.top,
                            sizePx = bounds.width,
                            gapPx = gapPx,
                            boardSize = boardSize,
                        ),
                    )
                },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(grid.gridSize) { row ->
                    val rowIsCell = row % 2 == 0
                    Row(
                        modifier = if (rowIsCell) Modifier.weight(1f) else Modifier.height(gap),
                    ) {
                        repeat(grid.gridSize) { col ->
                            val colIsCell = col % 2 == 0
                            val boxModifier = (if (colIsCell) Modifier.weight(1f) else Modifier.width(gap))
                                .fillMaxHeight()

                            when (val slot = grid.slotAt(row, col)) {
                                is CellSlot -> CellBox(
                                    modifier = boxModifier,
                                    slot = slot,
                                    targetColor = activePlayerColor,
                                    onActivePawnClick = onActivePawnClick,
                                    onCellClick = onCellClick,
                                )
                                is WallSlot -> Box(
                                    modifier = boxModifier.background(
                                        grooveColor(slot.isCovered, slot.isHighlighted),
                                    ),
                                )
                                is IntersectionSlot -> Box(
                                    modifier = boxModifier.background(
                                        grooveColor(slot.isCovered, slot.isHighlighted),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            previewWall?.let { wall ->
                WallPreview(wall = wall, step = step, cellSize = cellSize, gap = gap)
            }

            state.players.forEach { player ->
                GoalIndicator(player = player)
            }

            state.players.forEach { player ->
                AnimatedPawn(
                    player = player,
                    step = step,
                    cellSize = cellSize,
                    pawnSize = cellSize * 0.7f,
                )
            }
        }
    }
}

/** Color de una ranura/intersección: muro colocado, posición legal resaltada o fondo. */
@Composable
private fun grooveColor(isCovered: Boolean, isHighlighted: Boolean): Color {
    val boardColors = QuoridorTheme.boardColors
    return when {
        isCovered -> boardColors.wallPlaced
        isHighlighted -> boardColors.wallLegal.copy(alpha = LEGAL_WALL_ALPHA)
        else -> boardColors.background
    }
}

/** Vista previa (gris claro) del muro que se colocará al soltar el dedo. */
@Composable
private fun WallPreview(
    wall: Wall,
    step: Dp,
    cellSize: Dp,
    gap: Dp,
) {
    val length = cellSize * 2 + gap
    val bounds = when (wall.orientation) {
        WallOrientation.HORIZONTAL -> WallBounds(
            x = step * wall.col,
            y = step * wall.row + cellSize,
            width = length,
            height = gap,
        )
        WallOrientation.VERTICAL -> WallBounds(
            x = step * wall.col + cellSize,
            y = step * wall.row,
            width = gap,
            height = length,
        )
    }
    Box(
        modifier = Modifier
            .offset(x = bounds.x, y = bounds.y)
            .size(width = bounds.width, height = bounds.height)
            .clip(RoundedCornerShape(2.dp))
            .background(QuoridorTheme.boardColors.wallPreview),
    )
}

private data class WallBounds(val x: Dp, val y: Dp, val width: Dp, val height: Dp)

@Composable
private fun GoalIndicator(player: Player) {
    val thickness = 3.dp
    val margin = 6.dp
    val color = playerColor(player.id.value)
    val (modifier, alignment) = when (player.goalSide) {
        TOP -> Modifier.fillMaxWidth().height(thickness).offset(y = -(thickness + margin)) to Alignment.TopCenter
        BOTTOM -> Modifier.fillMaxWidth().height(thickness).offset(y = thickness + margin) to Alignment.BottomCenter
        LEFT -> Modifier.fillMaxHeight().width(thickness).offset(x = -(thickness + margin)) to Alignment.CenterStart
        RIGHT -> Modifier.fillMaxHeight().width(thickness).offset(x = thickness + margin) to Alignment.CenterEnd
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        Box(modifier = modifier.background(color))
    }
}

@Composable
private fun AnimatedPawn(
    player: Player,
    step: Dp,
    cellSize: Dp,
    pawnSize: Dp,
) {
    val inset = (cellSize - pawnSize) / 2
    val targetX = step * player.position.col + inset
    val targetY = step * player.position.row + inset
    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = tween(durationMillis = 300),
        label = "pawnX",
    )
    val animY by animateDpAsState(
        targetValue = targetY,
        animationSpec = tween(durationMillis = 300),
        label = "pawnY",
    )
    Box(
        modifier = Modifier
            .offset(x = animX, y = animY)
            .size(pawnSize)
            .clip(CircleShape)
            .background(playerColor(player.id.value)),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides QuoridorTheme.boardColors.pawnLabel) {
            Text(
                text = stringResource(Res.string.pawn_label, player.id.value + 1),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun CellBox(
    modifier: Modifier,
    slot: CellSlot,
    targetColor: Color,
    onActivePawnClick: () -> Unit,
    onCellClick: (Cell) -> Unit,
) {
    val boardColors = QuoridorTheme.boardColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    slot.isActivePawn -> boardColors.cellActive
                    slot.isPawnTarget -> boardColors.cellTarget
                    else -> boardColors.cell
                },
            )
            .clickable { if (slot.isActivePawn) onActivePawnClick() else onCellClick(slot.cell) },
        contentAlignment = Alignment.Center,
    ) {
        if (slot.occupant == null && slot.isPawnTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.22f)
                    .clip(CircleShape)
                    .background(targetColor),
            )
        }
    }
}

@Preview
@Composable
private fun BoardViewPreview() {
    QuoridorTheme {
        val state = QuoridorRules.startGame(GameConfig(playerCount = 2))
        BoardView(
            state = state,
            legalTargets = emptySet(),
            onActivePawnClick = {},
            onCellClick = {},
        )
    }
}

@Preview
@Composable
private fun BoardViewWallPreview() {
    QuoridorTheme {
        val state = QuoridorRules.startGame(GameConfig(playerCount = 2))
        BoardView(
            state = state,
            legalTargets = emptySet(),
            onActivePawnClick = {},
            onCellClick = {},
            previewWall = Wall(3, 3, WallOrientation.HORIZONTAL),
            highlightedWalls = QuoridorRules.getLegalMoves(state)
                .filterIsInstance<Move.PlaceWall>()
                .map { it.wall }
                .filterTo(mutableSetOf()) { it.orientation == WallOrientation.HORIZONTAL },
        )
    }
}
