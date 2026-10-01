package com.btello.quoridor.presentation.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.online.seriesFormatLabel
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.competitive_round_winner
import quoridor.app.shared.generated.resources.competitive_series_continue
import quoridor.app.shared.generated.resources.game_you_lost
import quoridor.app.shared.generated.resources.game_you_won

/**
 * Pantalla intermedia de una serie competitiva local (hot-seat o contra la IA):
 * se muestra al terminar una ronda que no cierra la serie. Anuncia quién ganó la
 * ronda, muestra el marcador reutilizando [CompetitivePanel] y ofrece continuar a
 * la siguiente ronda con [onContinue].
 *
 * [result] aporta la perspectiva del jugador humano cuando existe (contra la IA);
 * en hot-seat sin perspectiva local se anuncia al ganador por su número.
 */
@Composable
internal fun LocalSeriesResultScreen(
    competitive: CompetitiveUi,
    winnerNumber: Int,
    result: GameResult?,
    onContinue: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = seriesFormatLabel(competitive.format),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = roundOutcomeText(result, winnerNumber),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            CompetitivePanel(
                competitive = competitive,
                playerNames = emptyList(),
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.padding(top = 28.dp),
            ) {
                Text(
                    text = stringResource(Res.string.competitive_series_continue),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Texto del resultado de la ronda: perspectiva humana si existe, o el ganador por número. */
@Composable
private fun roundOutcomeText(result: GameResult?, winnerNumber: Int): String = when (result) {
    GameResult.WON -> stringResource(Res.string.game_you_won)
    GameResult.LOST -> stringResource(Res.string.game_you_lost)
    null -> stringResource(Res.string.competitive_round_winner, winnerNumber)
}

@Preview
@Composable
private fun LocalSeriesResultHotSeatPreview() {
    QuoridorTheme {
        LocalSeriesResultScreen(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(1, 0),
                gamesToWin = 3,
            ),
            winnerNumber = 1,
            result = null,
            onContinue = {},
        )
    }
}

@Preview
@Composable
private fun LocalSeriesResultVsAiPreview() {
    QuoridorTheme {
        LocalSeriesResultScreen(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(0, 1),
                gamesToWin = 3,
                localPlayerId = PlayerId(0),
            ),
            winnerNumber = 2,
            result = GameResult.LOST,
            onContinue = {},
        )
    }
}
