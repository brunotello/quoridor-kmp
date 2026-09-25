package com.btello.quoridor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.Destination.Game
import com.btello.quoridor.Destination.Home
import com.btello.quoridor.Destination.SettingsAbout
import com.btello.quoridor.presentation.game.GameScreen
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.navigation.NavAnimatedContent
import com.btello.quoridor.presentation.navigationbar.NavBar
import com.btello.quoridor.presentation.settings.AboutScreen
import com.btello.quoridor.presentation.settings.ProvideAppLocale
import com.btello.quoridor.presentation.settings.defaultLanguageCode
import com.btello.quoridor.presentation.theme.QuoridorTheme

private sealed interface Destination {
    data object Home : Destination
    data class Game(val setup: GameSetup, val sessionId: Int) : Destination
    data object SettingsAbout : Destination
}

@Composable
@Preview
fun App() {
    var darkTheme by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf(defaultLanguageCode(Locale.current.language)) }

    ProvideAppLocale(language) {
        QuoridorTheme(darkTheme = darkTheme) {
            var destination by remember { mutableStateOf<Destination>(Home) }
            var sessionId by remember { mutableStateOf(0) }

            NavAnimatedContent(
                targetState = destination,
                depthOf = { if (it is Home) 0 else 1 },
            ) { current ->
                when (current) {
                    Home -> NavBar(
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = it },
                        language = language,
                        onLanguageChange = { language = it },
                        onNavigateToGame = { setup ->
                            sessionId += 1
                            destination = Game(setup, sessionId)
                        },
                        onNavigateToAbout = { destination = SettingsAbout },
                    )
                    is Game -> GameScreen(
                        setup = current.setup,
                        sessionKey = current.sessionId,
                        onNavigateToMenu = { destination = Home },
                    )
                    SettingsAbout -> AboutScreen(
                        onBack = { destination = Home },
                    )
                }
            }
        }
    }
}
