package com.btello.quoridor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.btello.quoridor.presentation.rules.RulesScreen
import com.btello.quoridor.presentation.settings.AboutScreen
import com.btello.quoridor.presentation.settings.ProvideAppLocale
import com.btello.quoridor.presentation.settings.SettingsScreen
import com.btello.quoridor.presentation.theme.QuoridorTheme
import platform.UIKit.UIViewController

/**
 * Puntos de entrada para el `TabView` nativo (Liquid Glass) de iOS 26.
 *
 * Cada función devuelve un [UIViewController] que Swift hospeda en una pestaña.
 * El sistema es dueño de la barra; Compose sólo renderiza el contenido del tab.
 */

@Suppress("unused", "FunctionName")
fun GameTabViewController(): UIViewController = tabViewController { GameTab() }

@Suppress("unused", "FunctionName")
fun RulesTabViewController(): UIViewController = tabViewController { RulesScreen() }

@Suppress("unused", "FunctionName")
fun SettingsTabViewController(): UIViewController = tabViewController { SettingsTab() }

/**
 * Contenido de la pestaña "Ajustes": alterna entre [SettingsScreen] y
 * [AboutScreen], replicando la navegación a "Acerca de" que en Android maneja
 * `App()`, pero acotada a esta pestaña.
 */
@Composable
private fun SettingsTab() {
    var showAbout by remember { mutableStateOf(false) }
    val darkTheme by IosAppState.darkTheme
    val language by IosAppState.language
    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
    } else {
        SettingsScreen(
            darkTheme = darkTheme,
            onToggleTheme = { IosAppState.darkTheme.value = it },
            language = language,
            onLanguageChange = { IosAppState.language.value = it },
            onNavigateToAbout = { showAbout = true },
        )
    }
}

private fun tabViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController {
        val darkTheme by IosAppState.darkTheme
        val language by IosAppState.language
        ProvideAppLocale(language) {
            QuoridorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content()
                }
            }
        }
    }
