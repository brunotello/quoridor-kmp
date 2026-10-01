package com.btello.quoridor.presentation.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.online.seriesFormatLabel
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.playerColor
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.competitive_series_wins
import quoridor.app.shared.generated.resources.player_name

/**
 * Sección superior de una partida competitiva online: muestra las rondas y el
 * marcador (victorias de cada jugador). Se ubica sobre el indicador de turno; el
 * temporizador por turno se muestra debajo de ese indicador.
 */
@Composable
internal fun CompetitivePanel(
    competitive: CompetitiveUi,
    playerNames: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
       // color = MaterialTheme.colorScheme.surfaceVariant,
        //contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        //shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
                Text(
                    text = seriesFormatLabel(competitive.format),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                competitive.wins.indices
                    .filter { it in competitive.presentPlayerIds }
                    .forEach { index ->
                        CompetitivePlayerStat(
                            index = index,
                            name = playerNames.getOrNull(index)?.takeIf { it.isNotBlank() },
                            wins = competitive.wins.getOrNull(index) ?: 0,
                            gamesToWin = competitive.gamesToWin,
                            showWins = competitive.format != SeriesFormat.SINGLE,
                            isLocal = competitive.localPlayerId?.value == index,
                            modifier = Modifier.weight(1f),
                        )
                    }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun CompetitivePlayerStat(
    index: Int,
    name: String?,
    wins: Int,
    gamesToWin: Int,
    showWins: Boolean,
    isLocal: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = name ?: stringResource(Res.string.player_name, index + 1),
            style = MaterialTheme.typography.labelLarge,
            color = playerColor(index),
            fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        if (showWins) {
            Text(
                text = stringResource(Res.string.competitive_series_wins, wins, gamesToWin),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun CompetitivePanelSeriesPreview() {
    QuoridorTheme {
        CompetitivePanel(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(1, 0),
                gamesToWin = 3,
                turnRemainingMillis = 27_000L,
                localPlayerId = PlayerId(0),
            ),
            playerNames = listOf("Ana", "Beto"),
        )
    }
}

@Preview
@Composable
private fun CompetitivePanelPlayerLeftPreview() {
    QuoridorTheme {
        CompetitivePanel(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(1, 0, 2, 0),
                gamesToWin = 3,
                localPlayerId = PlayerId(2),
                presentPlayerIds = setOf(0, 2, 3),
            ),
            playerNames = listOf("Ana", "Beto", "Caro", "Dani"),
        )
    }
}

@Preview
@Composable
private fun CompetitivePanelTimerOnlyPreview() {
    QuoridorTheme {
        CompetitivePanel(
            competitive = CompetitiveUi(
                format = SeriesFormat.SINGLE,
                wins = listOf(0, 0),
                gamesToWin = 1,
                turnRemainingMillis = 12_000L,
                localPlayerId = PlayerId(1),
            ),
            playerNames = listOf("Ana", "Beto"),
        )
    }
}
