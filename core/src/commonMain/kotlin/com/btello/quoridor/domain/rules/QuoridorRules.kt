package com.btello.quoridor.domain.rules

import com.btello.quoridor.domain.model.Board
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.FOUR_PLAYER_WALLS
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.GameStatus
import com.btello.quoridor.domain.model.GoalSide
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.MoveResult
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.TWO_PLAYER_WALLS
import com.btello.quoridor.domain.model.Turn
import com.btello.quoridor.domain.model.ValidationResult
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallOrientation

object QuoridorRules {
    fun startGame(config: GameConfig): GameState {
        if (config.playerCount !in listOf(2, 4)) {
            throw DomainError.InvalidPlayerCount()
        }

        val players = buildPlayers(config.playerCount)
        return GameState(
            board = Board(size = config.boardSize),
            players = players,
            turn = Turn(players.first().id),
        )
    }

    fun startGame(playerCount: Int): GameState = startGame(GameConfig(playerCount = playerCount))

    fun validateMove(state: GameState, move: Move): ValidationResult {
        if (state.status == GameStatus.GAME_OVER) {
            return ValidationResult(false, DomainError.CellBlocked("The game is already over"))
        }
        if (move.playerId != state.turn.playerId) {
            return ValidationResult(false, DomainError.NotPlayersTurn())
        }

        val player = state.players.firstOrNull { it.id == move.playerId }
            ?: return ValidationResult(false, DomainError.OutOfBounds("Player not found"))

        return when (move) {
            is Move.PawnMove -> validatePawnMove(state, player, move)
            is Move.PlaceWall -> validateWallPlacement(state, player, move)
        }
    }

    fun applyMove(state: GameState, move: Move): MoveResult {
        val validation = validateMove(state, move)
        if (!validation.isValid) {
            return MoveResult(state = state, isSuccessful = false, error = validation.error)
        }

        return when (move) {
            is Move.PawnMove -> applyPawnMove(state, move)
            is Move.PlaceWall -> applyWallMove(state, move)
        }
    }

    fun getLegalMoves(state: GameState): List<Move> {
        val player = state.players.firstOrNull { it.id == state.turn.playerId } ?: return emptyList()
        val legalPawnMoves = getLegalPawnMoves(state, player)
        val legalWallMoves = getLegalWallMoves(state, player)
        return legalPawnMoves + legalWallMoves
    }

    fun isGameOver(state: GameState): Boolean =
        state.status == GameStatus.GAME_OVER || state.players.any { hasReachedGoal(state, it) }

    /**
     * Longitud (en pasos ortogonales) del camino más corto del jugador [playerId]
     * hasta su [GoalSide], respetando los muros del tablero. Ignora a los demás
     * peones (pueden moverse). Devuelve `null` si el jugador no existe o no tiene
     * camino a su meta.
     */
    fun shortestPathLength(state: GameState, playerId: PlayerId): Int? {
        val player = state.players.firstOrNull { it.id == playerId } ?: return null
        val board = state.board
        val queue = ArrayDeque<Cell>()
        val distance = mutableMapOf(player.position to 0)
        queue.add(player.position)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentDistance = distance.getValue(current)
            if (reachedGoal(board, player.goalSide, current)) return currentDistance
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nextRow = current.row + dr
                val nextCol = current.col + dc
                if (nextRow !in 0 until board.size || nextCol !in 0 until board.size) continue
                val next = Cell(nextRow, nextCol)
                if (next in distance) continue
                if (isWallBlocking(board, current, next)) continue
                distance[next] = currentDistance + 1
                queue.add(next)
            }
        }
        return null
    }

    private fun buildPlayers(playerCount: Int): List<Player> {
        val starterPositions = when (playerCount) {
            2 -> listOf(
                Player(PlayerId(0), Cell(0, 4), TWO_PLAYER_WALLS, GoalSide.BOTTOM),
                Player(PlayerId(1), Cell(8, 4), TWO_PLAYER_WALLS, GoalSide.TOP),
            )
            4 -> listOf(
                Player(PlayerId(0), Cell(0, 4), FOUR_PLAYER_WALLS, GoalSide.BOTTOM),
                Player(PlayerId(1), Cell(8, 4), FOUR_PLAYER_WALLS, GoalSide.TOP),
                Player(PlayerId(2), Cell(4, 0), FOUR_PLAYER_WALLS, GoalSide.RIGHT),
                Player(PlayerId(3), Cell(4, 8), FOUR_PLAYER_WALLS, GoalSide.LEFT),
            )
            else -> throw DomainError.InvalidPlayerCount()
        }
        return starterPositions
    }

    private fun validatePawnMove(state: GameState, player: Player, move: Move.PawnMove): ValidationResult {
        if (!state.board.contains(move.to)) {
            return ValidationResult(false, DomainError.OutOfBounds())
        }
        if (move.from != player.position) {
            return ValidationResult(false, DomainError.CellBlocked("Pawn is not in the expected position"))
        }
        val legalMoves = getLegalPawnMoves(state, player)
        return if (move.to in legalMoves.map { it.to }) {
            ValidationResult(true)
        } else {
            ValidationResult(false, DomainError.CellBlocked())
        }
    }

    private fun validateWallPlacement(state: GameState, player: Player, move: Move.PlaceWall): ValidationResult {
        if (player.wallsRemaining <= 0) {
            return ValidationResult(false, DomainError.NoWallsRemaining())
        }
        if (!state.board.isValidWall(move.wall)) {
            return ValidationResult(false, DomainError.OutOfBounds())
        }
        if (state.board.walls.any { overlaps(it, move.wall) }) {
            return ValidationResult(false, DomainError.WallOverlap())
        }
        if (hasWallCrossing(state.board.walls, move.wall)) {
            return ValidationResult(false, DomainError.WallCrossing())
        }
        val candidateBoard = state.board.copy(walls = state.board.walls + move.wall)
        for (candidatePlayer in state.players) {
            if (!hasPathToGoal(candidateBoard, candidatePlayer)) {
                return ValidationResult(false, DomainError.WallBlocksAllPaths())
            }
        }
        return ValidationResult(true)
    }

    private fun getLegalPawnMoves(state: GameState, player: Player): List<Move.PawnMove> {
        val moves = mutableListOf<Move.PawnMove>()
        val occupied = state.players.associate { other -> other.id to other.position }
        val directions = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        val diagonals = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

        for ((dr, dc) in directions) {
            val candidateRow = player.position.row + dr
            val candidateCol = player.position.col + dc
            if (candidateRow !in 0 until state.board.size || candidateCol !in 0 until state.board.size) continue
            val candidate = Cell(candidateRow, candidateCol)
            val occupiedByPlayer = occupied.values.any { it == candidate }
            if (!occupiedByPlayer && !isWallBlocking(state.board, player.position, candidate)) {
                moves += Move.PawnMove(player.id, player.position, candidate)
                continue
            }

            val opponent = state.players.firstOrNull { it.position == candidate }
            if (opponent != null) {
                val beyondRow = candidate.row + dr
                val beyondCol = candidate.col + dc
                if (beyondRow !in 0 until state.board.size || beyondCol !in 0 until state.board.size) continue
                val beyond = Cell(beyondRow, beyondCol)
                val behindBlocked = occupied.values.any { it == beyond } || isWallBlocking(state.board, candidate, beyond)
                if (!behindBlocked && !isWallBlocking(state.board, player.position, candidate)) {
                    moves += Move.PawnMove(player.id, player.position, beyond)
                }

                if (behindBlocked) {
                    for ((ddr, ddc) in diagonals) {
                        val diagonalRow = candidate.row + ddr
                        val diagonalCol = candidate.col + ddc
                        if (diagonalRow !in 0 until state.board.size || diagonalCol !in 0 until state.board.size) continue
                        val diagonalTarget = Cell(diagonalRow, diagonalCol)
                        if (occupied.values.any { it == diagonalTarget }) continue
                        val validDiagonal = !isWallBlocking(state.board, candidate, diagonalTarget)
                        if (validDiagonal) {
                            moves += Move.PawnMove(player.id, player.position, diagonalTarget)
                        }
                    }
                }
            }
        }

        return moves.distinctBy { it.to }
    }

    private fun getLegalWallMoves(state: GameState, player: Player): List<Move.PlaceWall> {
        if (player.wallsRemaining <= 0) return emptyList()
        val legalWalls = mutableListOf<Move.PlaceWall>()
        for (row in 0 until state.board.size - 1) {
            for (col in 0 until state.board.size - 1) {
                for (orientation in listOf(WallOrientation.HORIZONTAL, WallOrientation.VERTICAL)) {
                    val wall = Wall(row, col, orientation)
                    val validation = validateWallPlacement(state, player, Move.PlaceWall(player.id, wall))
                    if (validation.isValid) {
                        legalWalls += Move.PlaceWall(player.id, wall)
                    }
                }
            }
        }
        return legalWalls
    }

    private fun applyPawnMove(state: GameState, move: Move.PawnMove): MoveResult {
        val currentIndex = state.players.indexOfFirst { it.id == move.playerId }
        val nextPlayers = state.players.map { player ->
            if (player.id == move.playerId) {
                player.copy(position = move.to)
            } else {
                player
            }
        }
        val nextTurnPlayer = if (currentIndex >= 0) {
            val nextIndex = (currentIndex + 1) % nextPlayers.size
            nextPlayers[nextIndex].id
        } else {
            move.playerId
        }
        val won = nextPlayers.any { it.id == move.playerId && hasReachedGoal(state.copy(players = nextPlayers), it) }
        val nextState = state.copy(
            players = nextPlayers,
            turn = Turn(nextTurnPlayer),
            status = if (won) GameStatus.GAME_OVER else state.status,
            winner = if (won) move.playerId else state.winner,
        )
        return MoveResult(state = nextState, isSuccessful = true)
    }

    private fun applyWallMove(state: GameState, move: Move.PlaceWall): MoveResult {
        val nextPlayers = state.players.map { player ->
            if (player.id == move.playerId) {
                player.copy(wallsRemaining = player.wallsRemaining - 1)
            } else {
                player
            }
        }
        val currentIndex = state.players.indexOfFirst { it.id == move.playerId }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % nextPlayers.size else 0
        val nextState = state.copy(
            board = state.board.copy(walls = state.board.walls + move.wall),
            players = nextPlayers,
            turn = Turn(nextPlayers[nextIndex].id),
        )
        return MoveResult(state = nextState, isSuccessful = true)
    }

    private fun hasReachedGoal(state: GameState, player: Player): Boolean =
        reachedGoal(state.board, player.goalSide, player.position)

    private fun reachedGoal(board: Board, goalSide: GoalSide, position: Cell): Boolean =
        when (goalSide) {
            GoalSide.TOP -> position.row == 0
            GoalSide.BOTTOM -> position.row == board.size - 1
            GoalSide.LEFT -> position.col == 0
            GoalSide.RIGHT -> position.col == board.size - 1
        }

    private fun hasPathToGoal(board: Board, player: Player): Boolean {
        val queue = ArrayDeque<Cell>()
        val visited = mutableSetOf<Cell>()
        queue.add(player.position)
        visited.add(player.position)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (hasReachedGoal(GameState(board = board, players = listOf(player)), player.copy(position = current))) {
                return true
            }
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nextRow = current.row + dr
                val nextCol = current.col + dc
                if (nextRow !in 0 until board.size || nextCol !in 0 until board.size) continue
                val next = Cell(nextRow, nextCol)
                if (next in visited) continue
                if (isWallBlocking(board, current, next)) continue
                visited += next
                queue.add(next)
            }
        }
        return false
    }

    private fun isWallBlocking(board: Board, from: Cell, to: Cell): Boolean {
        if (from.row == to.row && from.col == to.col) return false
        if (from.row == to.row) {
            // Horizontal move: blocked by a vertical wall that spans this row.
            // A vertical wall at (r, c) covers rows r and r+1, so the move is blocked
            // by a wall anchored either at this row or the row above.
            val row = from.row
            val col = minOf(from.col, to.col)
            return board.walls.any {
                it.orientation == WallOrientation.VERTICAL &&
                    it.col == col &&
                    (it.row == row || it.row == row - 1)
            }
        }
        if (from.col == to.col) {
            // Vertical move: blocked by a horizontal wall that spans this column.
            // A horizontal wall at (r, c) covers columns c and c+1, so the move is blocked
            // by a wall anchored either at this column or the column to the left.
            val row = minOf(from.row, to.row)
            val col = from.col
            return board.walls.any {
                it.orientation == WallOrientation.HORIZONTAL &&
                    it.row == row &&
                    (it.col == col || it.col == col - 1)
            }
        }
        return false
    }

    private fun overlaps(existing: Wall, candidate: Wall): Boolean {
        if (existing.orientation != candidate.orientation) return false
        return when (candidate.orientation) {
            WallOrientation.HORIZONTAL ->
                existing.row == candidate.row && kotlin.math.abs(existing.col - candidate.col) <= 1
            WallOrientation.VERTICAL ->
                existing.col == candidate.col && kotlin.math.abs(existing.row - candidate.row) <= 1
        }
    }

    private fun hasWallCrossing(existing: Set<Wall>, crossing: Wall): Boolean =
        existing.any {
            it != crossing &&
                it.orientation != crossing.orientation &&
                it.row == crossing.row &&
                it.col == crossing.col
        }
}
