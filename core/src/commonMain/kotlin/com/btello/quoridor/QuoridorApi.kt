package com.btello.quoridor

import com.btello.quoridor.domain.model.Board
import com.btello.quoridor.domain.model.Cell
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import com.btello.quoridor.domain.model.Move
import com.btello.quoridor.domain.model.MoveResult
import com.btello.quoridor.domain.model.Player
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.model.PawnMove
import com.btello.quoridor.domain.model.Turn
import com.btello.quoridor.domain.model.ValidationResult
import com.btello.quoridor.domain.model.Wall
import com.btello.quoridor.domain.model.WallMove
import com.btello.quoridor.domain.model.WallOrientation
import com.btello.quoridor.domain.rules.DomainError
import com.btello.quoridor.domain.rules.QuoridorRules

fun startGame(config: GameConfig): GameState = QuoridorRules.startGame(config)
fun startGame(playerCount: Int): GameState = QuoridorRules.startGame(playerCount)
fun validateMove(state: GameState, move: Move): ValidationResult = QuoridorRules.validateMove(state, move)
fun applyMove(state: GameState, move: Move): MoveResult = QuoridorRules.applyMove(state, move)
fun getLegalMoves(state: GameState): List<Move> = QuoridorRules.getLegalMoves(state)
fun isGameOver(state: GameState): Boolean = QuoridorRules.isGameOver(state)

typealias BoardValue = Board
typealias CellValue = Cell
typealias PlayerValue = Player
typealias TurnValue = Turn
typealias MoveValue = Move
typealias WallValue = Wall
typealias WallOrientationValue = WallOrientation
typealias PlayerIdValue = PlayerId
typealias ValidationValue = ValidationResult
typealias MoveResultValue = MoveResult
typealias DomainErrorValue = DomainError

typealias GameStateValue = GameState
typealias PawnMoveValue = PawnMove
typealias WallMoveValue = WallMove
