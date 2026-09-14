package com.btello.quoridor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.Destination.Game
import com.btello.quoridor.Destination.Home
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.presentation.game.GameScreen
import com.btello.quoridor.presentation.navigationbar.NavBar
import com.btello.quoridor.presentation.theme.QuoridorTheme

private sealed interface Destination {
    data object Home : Destination
    data class Game(val config: GameConfig, val sessionId: Int) : Destination
}

@Composable
@Preview
fun App() {
    var darkTheme by remember { mutableStateOf(true) }

    QuoridorTheme(darkTheme = darkTheme) {
        var destination by remember { mutableStateOf<Destination>(Home) }
        var sessionId by remember { mutableStateOf(0) }

        when (val current = destination) {
            Home -> NavBar(
                darkTheme = darkTheme,
                onToggleTheme = { darkTheme = it },
                onNavigateToGame = { config ->
                    sessionId += 1
                    destination = Game(config, sessionId)
                },
            )
            is Game -> GameScreen(
                config = current.config,
                sessionKey = current.sessionId,
                onNavigateToMenu = { destination = Home },
            )
        }
    }
}
