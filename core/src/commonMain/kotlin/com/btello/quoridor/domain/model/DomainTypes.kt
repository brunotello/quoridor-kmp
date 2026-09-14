package com.btello.quoridor.domain.model

import com.btello.quoridor.domain.rules.DomainError
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

const val DEFAULT_BOARD_SIZE = 9
const val TWO_PLAYER_WALLS = 10
const val FOUR_PLAYER_WALLS = 5

@Serializable
@JvmInline
value class PlayerId(val value: Int) {
    init {
        require(value >= 0) { "Player id must be non-negative" }
    }

    override fun toString(): String = value.toString()
}

@Serializable
data class Cell(val row: Int, val col: Int) {
    init {
        require(row >= 0) { "Row must be non-negative" }
        require(col >= 0) { "Column must be non-negative" }
    }
}

typealias PawnPosition = Cell

@Serializable
enum class WallOrientation {
    HORIZONTAL,
    VERTICAL,
}

@Serializable
data class Wall(val row: Int, val col: Int, val orientation: WallOrientation) {
    init {
        require(row >= 0) { "Wall row must be non-negative" }
        require(col >= 0) { "Wall column must be non-negative" }
    }
}

@Serializable
data class Board(
    val size: Int = DEFAULT_BOARD_SIZE,
    val walls: Set<Wall> = emptySet(),
) {
    init {
        require(size > 0) { "Board size must be positive" }
    }

    fun contains(cell: Cell): Boolean =
        cell.row in 0 until size && cell.col in 0 until size

    fun isValidWall(wall: Wall): Boolean {
        val maxIndex = size - 2
        return wall.row in 0..maxIndex && wall.col in 0..maxIndex
    }
}

@Serializable
data class Turn(val playerId: PlayerId)

@Serializable
enum class GoalSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
}

@Serializable
data class Player(
    val id: PlayerId,
    val position: PawnPosition,
    val wallsRemaining: Int = 0,
    val goalSide: GoalSide,
)

@Serializable
enum class GameStatus {
    IN_PROGRESS,
    GAME_OVER,
}

@Serializable
data class GameState(
    val board: Board = Board(),
    val players: List<Player> = emptyList(),
    val turn: Turn = Turn(PlayerId(0)),
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winner: PlayerId? = null,
)

@Serializable
sealed interface Move {
    val playerId: PlayerId

    @Serializable
    data class PawnMove(
        override val playerId: PlayerId,
        val from: PawnPosition,
        val to: PawnPosition,
    ) : Move

    @Serializable
    data class PlaceWall(
        override val playerId: PlayerId,
        val wall: Wall,
    ) : Move
}

typealias PawnMove = Move.PawnMove
typealias WallMove = Move.PlaceWall

@Serializable
data class GameConfig(
    val playerCount: Int = 2,
    val boardSize: Int = DEFAULT_BOARD_SIZE,
) {
    init {
        require(playerCount == 2 || playerCount == 4) { "Player count must be 2 or 4" }
        require(boardSize > 0) { "Board size must be positive" }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val error: DomainError? = null,
)

data class MoveResult(
    val state: GameState? = null,
    val isSuccessful: Boolean = false,
    val error: DomainError? = null,
)
