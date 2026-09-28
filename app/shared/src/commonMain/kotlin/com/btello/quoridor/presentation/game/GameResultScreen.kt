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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.game_over
import quoridor.app.shared.generated.resources.game_result_sad_emoji
import quoridor.app.shared.generated.resources.game_result_trophy_emoji
import quoridor.app.shared.generated.resources.game_you_lost
import quoridor.app.shared.generated.resources.game_you_won
import quoridor.app.shared.generated.resources.game_back_to_menu
import quoridor.app.shared.generated.resources.game_result_opponent_left
import quoridor.app.shared.generated.resources.winner

@Composable
internal fun GameResultScreen(
    winnerNumber: Int,
    onBackToMenu: () -> Unit,
    result: GameResult? = null,
    isAbandoned: Boolean = false,
    onContinue: (() -> Unit)? = null,
) {
    val backAction = remember(onBackToMenu, onContinue) {
        onContinue ?: onBackToMenu
    }
    val localWon = result == GameResult.WON || (isAbandoned && result == null)
    val localLost = result == GameResult.LOST
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (localWon || localLost) {
                Text(
                    text = stringResource(
                        if (localWon) Res.string.game_result_trophy_emoji else Res.string.game_result_sad_emoji,
                    ),
                    style = MaterialTheme.typography.displayLarge,
                )
            }
            Text(
                text = when {
                    localWon -> stringResource(Res.string.game_you_won)
                    localLost -> stringResource(Res.string.game_you_lost)
                    else -> stringResource(Res.string.game_over)
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (isAbandoned || result == null) {
                Text(
                    text = if (isAbandoned) stringResource(Res.string.game_result_opponent_left)
                    else stringResource(Res.string.winner, winnerNumber),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Button(
                onClick = backAction,
                modifier = Modifier.padding(top = 28.dp),
            ) {
                Text(
                    text = stringResource(Res.string.game_back_to_menu),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameResultScreenWonPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 1, onBackToMenu = {}, result = GameResult.WON)
    }
}

@Preview
@Composable
private fun GameResultScreenLostPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 2, onBackToMenu = {}, result = GameResult.LOST)
    }
}

@Preview
@Composable
private fun GameResultScreenNeutralPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 1, onBackToMenu = {})
    }
}

@Preview
@Composable
private fun GameResultScreenAbandonedPreview() {
    QuoridorTheme {
        GameResultScreen(winnerNumber = 1, onBackToMenu = {}, isAbandoned = true)
    }
}
