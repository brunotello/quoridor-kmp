package com.btello.quoridor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.Destination.Game
import com.btello.quoridor.Destination.Menu
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.presentation.game.GameScreen
import com.btello.quoridor.presentation.menu.MenuScreen
import com.btello.quoridor.presentation.theme.QuoridorTheme

private sealed interface Destination {
    data object Menu : Destination
    data class Game(val config: GameConfig, val sessionId: Int) : Destination
}

@Composable
@Preview
fun App() {
    QuoridorTheme {
        var destination by remember { mutableStateOf<Destination>(Menu) }
        var sessionId by remember { mutableStateOf(0) }

        when (val current = destination) {
            Menu -> MenuScreen(
                onNavigateToGame = { config ->
                    sessionId += 1
                    destination = Game(config, sessionId)
                },
            )
            is Game -> GameScreen(
                config = current.config,
                sessionKey = current.sessionId,
                onNavigateToMenu = { destination = Menu },
            )
        }
    }
}
