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
import quoridor.app.shared.generated.resources.competitive_series_continue
import quoridor.app.shared.generated.resources.competitive_series_game_over
import quoridor.app.shared.generated.resources.competitive_series_score
import quoridor.app.shared.generated.resources.competitive_series_waiting
import quoridor.app.shared.generated.resources.game_you_lost
import quoridor.app.shared.generated.resources.game_you_won

/**
 * Pantalla intermedia de una serie competitiva online: se muestra al terminar un
 * juego que no cierra la serie. Indica el resultado del juego, el marcador de la
 * serie y ofrece continuar al ganador del juego, mientras el rival espera. La
 * continuación se dispara con [GameEvent.ContinueSeries].
 */
@Composable
internal fun SeriesResultScreen(
    competitive: CompetitiveUi,
    result: GameResult?,
    onContinue: () -> Unit,
) {
    val localWon = result == GameResult.WON
    val localIndex = competitive.localPlayerId?.value ?: 0
    val localWins = competitive.wins.getOrNull(localIndex) ?: 0
    val opponentWins = competitive.wins
        .filterIndexed { index, _ -> index != localIndex }
        .maxOrNull() ?: 0
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
                text = if (localWon) {
                    stringResource(Res.string.game_you_won)
                } else {
                    stringResource(Res.string.game_you_lost)
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(Res.string.competitive_series_score, localWins, opponentWins),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (localWon) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 28.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.competitive_series_continue),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.competitive_series_waiting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SeriesResultScreenWinnerPreview() {
    QuoridorTheme {
        SeriesResultScreen(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(1, 0),
                gamesToWin = 3,
                localPlayerId = PlayerId(0),
            ),
            result = GameResult.WON,
            onContinue = {},
        )
    }
}

@Preview
@Composable
private fun SeriesResultScreenLoserPreview() {
    QuoridorTheme {
        SeriesResultScreen(
            competitive = CompetitiveUi(
                format = SeriesFormat.FIRST_TO_3,
                wins = listOf(1, 0),
                gamesToWin = 3,
                localPlayerId = PlayerId(1),
            ),
            result = GameResult.LOST,
            onContinue = {},
        )
    }
}
