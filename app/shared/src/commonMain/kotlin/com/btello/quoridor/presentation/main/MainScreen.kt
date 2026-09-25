package com.btello.quoridor.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.navigation.NavAnimatedContent
import com.btello.quoridor.presentation.online.OnlineLobbyScreen
import com.btello.quoridor.presentation.theme.EmojiText
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.app_title
import quoridor.app.shared.generated.resources.coming_soon
import quoridor.app.shared.generated.resources.new_game_headline

@Composable
internal fun MainScreen(
    onNavigateToGame: (GameSetup) -> Unit,
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    var difficultySelection by remember { mutableStateOf<DifficultySelection?>(null) }
    var playerSetupMode by remember { mutableStateOf<GameMode?>(null) }
    var showOnlineLobby by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is MainSideEffect.NavigateToGame -> {
                    difficultySelection = null
                    playerSetupMode = null
                    showOnlineLobby = false
                    onNavigateToGame(effect.setup)
                }

                is MainSideEffect.NavigateToDifficulty ->
                    difficultySelection = DifficultySelection(effect.mode, effect.aiCount)

                is MainSideEffect.NavigateToPlayerSetup -> playerSetupMode = effect.mode

                MainSideEffect.NavigateToOnlineLobby -> showOnlineLobby = true
            }
        }
    }

    val selection = difficultySelection
    val setupMode = playerSetupMode
    val target: MainNav = when {
        selection != null -> MainNav.Difficulty(selection)
        setupMode != null -> MainNav.PlayerSetup(setupMode)
        showOnlineLobby -> MainNav.OnlineLobby
        else -> MainNav.Content
    }

    NavAnimatedContent(
        targetState = target,
        depthOf = { if (it is MainNav.Content) 0 else 1 },
    ) { current ->
        when (current) {
            is MainNav.Difficulty -> {
                AppBackHandler { difficultySelection = null }
                DifficultyScreen(
                    onSelectDifficulty = { option ->
                        viewModel.onEvent(
                            MainEvent.SelectDifficulty(
                                current.selection.mode,
                                current.selection.aiCount,
                                option,
                            ),
                        )
                    },
                    onBack = { difficultySelection = null },
                )
            }

            is MainNav.PlayerSetup -> {
                AppBackHandler { playerSetupMode = null }
                PlayerSetupScreen(
                    onSelectSetup = { option ->
                        viewModel.onEvent(MainEvent.SelectPlayerSetup(current.mode, option))
                    },
                    onBack = { playerSetupMode = null },
                )
            }

            MainNav.OnlineLobby -> {
                AppBackHandler { showOnlineLobby = false }
                OnlineLobbyScreen(
                    onStartGame = { setup ->
                        showOnlineLobby = false
                        onNavigateToGame(setup)
                    },
                    onBack = { showOnlineLobby = false },
                )
            }

            MainNav.Content -> MainContent(state = viewModel.uiState, onEvent = viewModel::onEvent)
        }
    }
}

/** Destino interno de [MainScreen], usado para animar la navegación. */
private sealed interface MainNav {
    data object Content : MainNav
    data class Difficulty(val selection: DifficultySelection) : MainNav
    data class PlayerSetup(val mode: GameMode) : MainNav
    data object OnlineLobby : MainNav
}

/** Modo elegido más la cantidad de jugadores IA, pendiente de elegir dificultad. */
private data class DifficultySelection(val mode: GameMode, val aiCount: Int)

@Composable
private fun MainContent(
    state: MainUiState,
    onEvent: (MainEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeAreaTopPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(Res.string.app_title),
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        ),
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
                Text(
                    text = stringResource(Res.string.new_game_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Start,
                )
            }

            state.modes.forEach { mode ->
                GameModeCard(
                    mode = mode,
                    onClick = { onEvent(MainEvent.SelectMode(mode)) },
                )
            }
        }
    }
}

@Composable
private fun GameModeCard(
    mode: GameMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = mode.enabled, onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EmojiText(
                text = stringResource(mode.emojiRes),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.size(40.dp).wrapContentSize(Alignment.Center),
            )
            Text(
                text = stringResource(mode.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (mode.enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(Res.string.coming_soon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview
@Composable
private fun MainContentPreview() {
    QuoridorTheme {
        MainContent(state = MainUiState(), onEvent = {})
    }
}
