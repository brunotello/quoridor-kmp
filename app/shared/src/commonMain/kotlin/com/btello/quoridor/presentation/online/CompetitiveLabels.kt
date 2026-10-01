package com.btello.quoridor.presentation.online

import androidx.compose.runtime.Composable
import com.btello.quoridor.domain.online.SeriesFormat
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.competitive_format_first_to_3
import quoridor.app.shared.generated.resources.competitive_format_first_to_5
import quoridor.app.shared.generated.resources.competitive_format_single
import quoridor.app.shared.generated.resources.competitive_timer_seconds
import quoridor.app.shared.generated.resources.competitive_timer_off

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE_L = 60L

/** Etiqueta legible de la cantidad de rondas competitivas. */
@Composable
internal fun seriesFormatLabel(format: SeriesFormat): String = when (format) {
    SeriesFormat.SINGLE -> stringResource(Res.string.competitive_format_single)
    SeriesFormat.FIRST_TO_3 -> stringResource(Res.string.competitive_format_first_to_3)
    SeriesFormat.FIRST_TO_5 -> stringResource(Res.string.competitive_format_first_to_5)
}

/** Etiqueta del tiempo por turno en segundos (o "sin límite" si es `null`). */
@Composable
internal fun turnTimeLabel(seconds: Int?): String =
    if (seconds == null) {
        stringResource(Res.string.competitive_timer_off)
    } else {
        stringResource(Res.string.competitive_timer_seconds, seconds)
    }

/** Formatea un tiempo en milisegundos como `m:ss` (nunca negativo). */
internal fun formatClock(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE_L
    val seconds = totalSeconds % SECONDS_PER_MINUTE_L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
