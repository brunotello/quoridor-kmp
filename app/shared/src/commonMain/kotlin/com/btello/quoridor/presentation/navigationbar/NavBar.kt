package com.btello.quoridor.presentation.navigationbar

import androidx.compose.foundation.layout.Box
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
import com.btello.quoridor.presentation.navigationbar.Tabs.SETTINGS
import com.btello.quoridor.presentation.main.MainScreen
import com.btello.quoridor.presentation.rules.RulesScreen
import com.btello.quoridor.presentation.settings.SettingsScreen
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
    onNavigateToGame: (GameSetup) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(GAME) }

    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                GAME -> MainScreen(onNavigateToGame = onNavigateToGame)
                RULES -> RulesScreen()
                SETTINGS -> SettingsScreen(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
            }
        }
    }
}

@Preview
@Composable
private fun NavBarPreview() {
    QuoridorTheme {
        NavBar(darkTheme = true, onToggleTheme = {}, onNavigateToGame = {})
    }
}
