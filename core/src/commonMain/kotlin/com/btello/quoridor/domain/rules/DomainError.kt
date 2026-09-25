package com.btello.quoridor.domain.rules


sealed class DomainError(message: String) : IllegalArgumentException(message) {
    class OutOfBounds(message: String = "Cell is outside the board") : DomainError(message)
    class CellBlocked(message: String = "Cell is blocked") : DomainError(message)
    class NotPlayersTurn(message: String = "It is not this player's turn") : DomainError(message)
    class WallOverlap(message: String = "Wall overlaps another wall") : DomainError(message)
    class WallCrossing(message: String = "Wall crosses another wall") : DomainError(message)
    class WallBlocksAllPaths(message: String = "This wall blocks all paths") : DomainError(message)
    class NoWallsRemaining(message: String = "Player has no walls remaining") : DomainError(message)
    class InvalidPlayerCount(message: String = "Player count must be 2 or 4") : DomainError(message)
}
