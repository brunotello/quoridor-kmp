package com.btello.quoridor.presentation.game

/**
 * Feedback de juego expuesto por el [GameViewModel].
 *
 * La capa Compose resuelve cada caso a un texto de `strings.xml`; la capa
 * de presentación no depende de `stringResource`.
 */
internal sealed interface GameFeedback {
    data object NoWallsRemaining : GameFeedback
    data object NoLegalWalls : GameFeedback
    data object InvalidWall : GameFeedback
    data object InvalidMove : GameFeedback
    data object GameOver : GameFeedback

    /** Mensaje proveniente del dominio (p. ej. [com.btello.quoridor.domain.rules.DomainError]). */
    data class DomainMessage(val text: String) : GameFeedback
}
