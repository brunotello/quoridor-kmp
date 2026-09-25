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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.new_game
import quoridor.app.shared.generated.resources.online_opponent_left
import quoridor.app.shared.generated.resources.winner

@Composable
internal fun GameResultScreen(
    winnerNumber: Int,
    onNewGame: () -> Unit,
    isAbandoned: Boolean = false,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.game_over),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = if (isAbandoned) {
                    stringResource(Res.string.online_opponent_left)
                } else {
                    stringResource(Res.string.winner, winnerNumber)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = onNewGame,
                modifier = Modifier.padding(top = 28.dp),
            ) {
                Text(
                    text = stringResource(Res.string.new_game),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameResultScreenPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 1, onNewGame = {})
    }
}

@Preview
@Composable
private fun GameResultScreenAbandonedPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 1, onNewGame = {}, isAbandoned = true)
    }
}
