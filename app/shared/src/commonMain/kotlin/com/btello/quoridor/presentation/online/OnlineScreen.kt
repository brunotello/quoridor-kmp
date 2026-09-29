package com.btello.quoridor.presentation.online

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.navigation.NavAnimatedContent
import com.btello.quoridor.presentation.online.create.CreateMatchOnlineScreen
import com.btello.quoridor.presentation.online.find.FindMatchesOnlineScreen
import com.btello.quoridor.presentation.online.join.JoinMatchOnlineScreen
import com.btello.quoridor.presentation.online.menu.OnlineMenuScreen

/**
 * Punto de entrada del modo online. Muestra la pantalla principal con las tres
 * opciones (crear, unirse, buscar) y navega a la pantalla correspondiente al
 * elegir una, cada una con su propio ViewModel. Al quedar una sala lista arranca
 * la partida mediante [onStartGame]; [onBack] sale del modo online.
 */
@Composable
internal fun OnlineScreen(
    onStartGame: (GameSetup) -> Unit,
    onBack: () -> Unit,
    key: Int = 0,
) {
    var destination by remember(key) { mutableStateOf(OnlineDestination.Menu) }
    val backToMenu: () -> Unit = { destination = OnlineDestination.Menu }

    AppBackHandler {
        if (destination == OnlineDestination.Menu) onBack() else backToMenu()
    }

    NavAnimatedContent(
        targetState = destination,
        depthOf = { if (it == OnlineDestination.Menu) 0 else 1 },
    ) { current ->
        when (current) {
            OnlineDestination.Menu -> OnlineMenuScreen(
                onCreate = { destination = OnlineDestination.Create },
                onJoin = { destination = OnlineDestination.Join },
                onFind = { destination = OnlineDestination.Find },
                onBack = onBack,
            )

            OnlineDestination.Create -> CreateMatchOnlineScreen(
                onStartGame = onStartGame,
                onBack = backToMenu,
            )

            OnlineDestination.Join -> JoinMatchOnlineScreen(
                onStartGame = onStartGame,
                onBack = backToMenu,
            )

            OnlineDestination.Find -> FindMatchesOnlineScreen(
                onStartGame = onStartGame,
                onBack = backToMenu,
            )
        }
    }
}

/** Destinos internos del modo online, usados para animar la navegación. */
private enum class OnlineDestination {
    Menu,
    Create,
    Join,
    Find,
}
