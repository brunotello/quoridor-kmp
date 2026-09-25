package com.btello.quoridor

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Fallback para iOS anterior a 26 (sin Liquid Glass): renderiza la app completa en
 * Compose, incluyendo la bottom navigation Material (`NavBar`). En iOS 26 la barra
 * la provee el `TabView` nativo (ver [TabViewControllers]).
 */
fun MainViewController() = ComposeUIViewController { App() }