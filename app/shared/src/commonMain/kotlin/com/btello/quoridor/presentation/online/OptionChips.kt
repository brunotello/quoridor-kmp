package com.btello.quoridor.presentation.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.SeriesFormat
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.competitive_format_label
import quoridor.app.shared.generated.resources.competitive_timer_label
import quoridor.app.shared.generated.resources.online_player_count

/**
 * Tiempos por turno (en segundos) ofrecidos para el temporizador competitivo.
 * `null` desactiva el temporizador (sin límite).
 */
internal val TURN_TIME_OPTIONS: List<Int?> = listOf(null) + CompetitiveConfig.TURN_TIME_OPTIONS

/**
 * Sección de opciones con encabezado (icono + título) y una fila de chips. Base
 * reutilizada por las pantallas de configuración (sala online y partida local).
 */
@Composable
internal fun OptionChipSection(
    label: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OptionSectionHeader(label = label, icon = icon)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

/** Encabezado de una sección de opciones: icono y título. */
@Composable
internal fun OptionSectionHeader(
    label: String,
    icon: ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Chips de selección de la cantidad de jugadores entre las [options] ofrecidas. */
@Composable
internal fun PlayerCountChips(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    OptionChipSection(
        label = stringResource(Res.string.online_player_count),
        icon = Icons.Filled.Group,
    ) {
        options.forEach { count ->
            FilterChip(
                selected = selected == count,
                onClick = { onSelect(count) },
                label = {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/** Chips de selección de la cantidad de rondas competitivas. */
@Composable
internal fun SeriesFormatChips(
    selected: SeriesFormat,
    onSelect: (SeriesFormat) -> Unit,
) {
    OptionChipSection(
        label = stringResource(Res.string.competitive_format_label),
        icon = Icons.Filled.Repeat,
    ) {
        SeriesFormat.entries.forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = {
                    Text(
                        text = seriesFormatLabel(format),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/** Chips de selección del tiempo por turno (o "sin límite"). */
@Composable
internal fun TurnTimeChips(
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    OptionChipSection(
        label = stringResource(Res.string.competitive_timer_label),
        icon = Icons.Filled.Timer,
    ) {
        TURN_TIME_OPTIONS.forEach { seconds ->
            FilterChip(
                selected = selected == seconds,
                onClick = { onSelect(seconds) },
                label = {
                    Text(
                        text = turnTimeLabel(seconds),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
