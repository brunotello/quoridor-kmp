package com.btello.quoridor.presentation.navigationbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.navigationbar.Tabs.GAME
import com.btello.quoridor.presentation.navigationbar.Tabs.RULES
import com.btello.quoridor.presentation.navigationbar.Tabs.STATS
import com.btello.quoridor.presentation.navigationbar.Tabs.SETTINGS
import com.btello.quoridor.presentation.main.MainScreen
import com.btello.quoridor.presentation.navigation.NavFadeThroughContent
import com.btello.quoridor.presentation.rules.RulesScreen
import com.btello.quoridor.presentation.settings.LANGUAGE_SPANISH
import com.btello.quoridor.presentation.settings.SettingsScreen
import com.btello.quoridor.presentation.stats.StatisticsScreen
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Pantalla principal con bottom navigation.
 *
 * Aloja las pestañas declaradas en [Tabs] y delega en cada feature su
 * contenido. Iniciar una partida navega fuera de este contenedor.
 */
@Composable
internal fun NavBar(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onNavigateToGame: (GameSetup) -> Unit,
    onNavigateToAbout: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(GAME) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tabs.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavFadeThroughContent(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { tab ->
            when (tab) {
                GAME -> MainScreen(onNavigateToGame = onNavigateToGame)
                RULES -> RulesScreen()
                STATS -> StatisticsScreen()
                SETTINGS -> SettingsScreen(
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    language = language,
                    onLanguageChange = onLanguageChange,
                    onNavigateToAbout = onNavigateToAbout,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NavBarPreview() {
    QuoridorTheme {
        NavBar(
            darkTheme = true,
            onToggleTheme = {},
            language = LANGUAGE_SPANISH,
            onLanguageChange = {},
            onNavigateToGame = {},
            onNavigateToAbout = {},
        )
    }
}
