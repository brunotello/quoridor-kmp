package com.btello.quoridor.presentation.online

import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.SeriesFormat

/**
 * Resumen de una sala pública mostrada en la búsqueda para unirse sin código.
 * Incluye la configuración competitiva ([format] de la serie y [timeControlSeconds]
 * del temporizador, `null` si no hay reloj) para que se vea cómo está armada.
 */
internal data class OnlineOpenMatch(
    val id: MatchId,
    val hostName: String,
    val joinedCount: Int,
    val playerCount: Int,
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val timeControlSeconds: Int? = null,
)
