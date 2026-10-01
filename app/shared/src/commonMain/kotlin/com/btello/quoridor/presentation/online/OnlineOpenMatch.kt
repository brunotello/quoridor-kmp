package com.btello.quoridor.presentation.online

import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.domain.online.SeriesFormat

/**
 * Resumen de una sala pública mostrada en la búsqueda para unirse sin código.
 * Incluye la configuración competitiva ([format] con las rondas y [turnTimeSeconds]
 * del temporizador por turno, `null` si no hay temporizador) para que se vea cómo está armada.
 */
internal data class OnlineOpenMatch(
    val id: MatchId,
    val hostName: String,
    val joinedCount: Int,
    val playerCount: Int,
    val format: SeriesFormat = SeriesFormat.SINGLE,
    val turnTimeSeconds: Int? = null,
)
