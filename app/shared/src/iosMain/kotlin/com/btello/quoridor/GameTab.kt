package com.btello.quoridor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.presentation.game.GameScreen
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.main.MainScreen
import com.btello.quoridor.presentation.theme.QuoridorTheme

/**
 * Contenido de la pestaña "Juego" para la barra Liquid Glass de iOS.
 *
 * Aloja el flujo de partida (menú → [GameScreen] → volver) dentro de la propia
 * pestaña, replicando el switching de destinos que hace `App()` pero acotado a
 * este tab, de modo que la barra nativa siga siendo dueña de la navegación.
 */
@Composable
internal fun GameTab() {
    var setup by remember { mutableStateOf<GameSetup?>(null) }
    var sessionId by remember { mutableStateOf(0) }

    val current = setup
    if (current == null) {
        MainScreen(
            onNavigateToGame = { newSetup ->
                sessionId += 1
                setup = newSetup
            },
        )
    } else {
        GameScreen(
            setup = current,
            sessionKey = sessionId,
            onNavigateToMenu = { setup = null },
        )
    }
}

@Preview
@Composable
private fun GameTabPreview() {
    QuoridorTheme {
        GameTab()
    }
}
