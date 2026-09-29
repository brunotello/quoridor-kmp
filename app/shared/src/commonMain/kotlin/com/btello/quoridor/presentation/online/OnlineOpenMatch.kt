package com.btello.quoridor.presentation.online

import com.btello.quoridor.domain.online.MatchId

/**
 * Resumen de una sala pública mostrada en la búsqueda para unirse sin código.
 */
internal data class OnlineOpenMatch(
    val id: MatchId,
    val hostName: String,
    val joinedCount: Int,
    val playerCount: Int,
)
