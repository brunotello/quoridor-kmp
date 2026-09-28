package com.btello.quoridor.presentation.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.domain.online.MatchId
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.navigation.AppBackHandler
import com.btello.quoridor.presentation.navigation.NavAnimatedContent
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.difficulty_back
import quoridor.app.shared.generated.resources.online_browse_description
import quoridor.app.shared.generated.resources.online_browse_empty
import quoridor.app.shared.generated.resources.online_browse_match
import quoridor.app.shared.generated.resources.online_browse_refresh
import quoridor.app.shared.generated.resources.online_cancel
import quoridor.app.shared.generated.resources.online_continue
import quoridor.app.shared.generated.resources.online_create_description
import quoridor.app.shared.generated.resources.online_create_match
import quoridor.app.shared.generated.resources.online_error_connection
import quoridor.app.shared.generated.resources.online_error_incompatible_version
import quoridor.app.shared.generated.resources.online_error_not_found
import quoridor.app.shared.generated.resources.online_error_not_joinable
import quoridor.app.shared.generated.resources.online_error_unsupported
import quoridor.app.shared.generated.resources.online_join_description
import quoridor.app.shared.generated.resources.online_join_hint
import quoridor.app.shared.generated.resources.online_join_match
import quoridor.app.shared.generated.resources.online_copy_code
import quoridor.app.shared.generated.resources.online_menu_headline
import quoridor.app.shared.generated.resources.online_name_headline
import quoridor.app.shared.generated.resources.online_open_match_host
import quoridor.app.shared.generated.resources.online_open_match_players
import quoridor.app.shared.generated.resources.online_player_count
import quoridor.app.shared.generated.resources.online_public_description
import quoridor.app.shared.generated.resources.online_public_label
import quoridor.app.shared.generated.resources.online_share_code
import quoridor.app.shared.generated.resources.online_waiting_opponent
import quoridor.app.shared.generated.resources.online_waiting_players
import quoridor.app.shared.generated.resources.online_your_name

/**
 * Lobby del modo online, dividido en pasos ([OnlineLobbyStep]): primero se pide
 * el nombre del jugador (si aún no lo definió), luego se elige entre crear una
 * sala o unirse a una existente, y cada acción tiene su propia pantalla. Al
 * quedar la sala lista arranca la partida.
 */
@Composable
internal fun OnlineLobbyScreen(
    onStartGame: (GameSetup) -> Unit,
    onBack: () -> Unit,
    key: Int = 0,
    viewModel: OnlineLobbyViewModel = viewModel(key = key.toString()) { OnlineLobbyViewModel() },
) {
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is OnlineLobbySideEffect.StartGame -> onStartGame(effect.setup)
            }
        }
    }

    val state = viewModel.uiState
    val onStepBack: () -> Unit = {
        when (state.step) {
            OnlineLobbyStep.Name, OnlineLobbyStep.Menu -> onBack()
            OnlineLobbyStep.Create, OnlineLobbyStep.Join, OnlineLobbyStep.Browse ->
                viewModel.onEvent(OnlineLobbyEvent.NavigateBack)
        }
    }

    AppBackHandler { onStepBack() }

    NavAnimatedContent(
        targetState = state.step,
        depthOf = { it.ordinal },
    ) { step ->
        when (step) {
            OnlineLobbyStep.Name -> NameStep(state = state, onEvent = viewModel::onEvent, onBack = onStepBack)
            OnlineLobbyStep.Menu -> MenuStep(onEvent = viewModel::onEvent, onBack = onStepBack)
            OnlineLobbyStep.Create -> CreateStep(state = state, onEvent = viewModel::onEvent, onBack = onStepBack)
            OnlineLobbyStep.Join -> JoinStep(state = state, onEvent = viewModel::onEvent, onBack = onStepBack)
            OnlineLobbyStep.Browse -> BrowseStep(state = state, onEvent = viewModel::onEvent, onBack = onStepBack)
        }
    }
}

/**
 * Andamiaje común de las pantallas del lobby: fondo, título, botón de volver y un
 * [content] centrado. Reutiliza el mismo estilo que el resto de la aplicación.
 */
@Composable
private fun OnlineStepScaffold(
    title: String,
    onBack: () -> Unit,
    action: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize().safeAreaTopPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                content()
            }
            BackButton(onBack)
            action?.invoke(this)
        }
    }
}

@Composable
private fun BoxScope.BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.align(Alignment.TopStart),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.difficulty_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun NameStep(
    state: OnlineLobbyUiState,
    onEvent: (OnlineLobbyEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_name_headline), onBack = onBack) {
        OutlinedTextField(
            value = state.playerName,
            onValueChange = { onEvent(OnlineLobbyEvent.NameChanged(it)) },
            singleLine = true,
            label = {
                Text(
                    text = stringResource(Res.string.online_your_name),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onEvent(OnlineLobbyEvent.ConfirmName) },
            enabled = state.canConfirmName,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.online_continue),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun MenuStep(
    onEvent: (OnlineLobbyEvent) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize().safeAreaTopPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(Res.string.online_menu_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                OnlineActionCard(
                    title = stringResource(Res.string.online_create_match),
                    description = stringResource(Res.string.online_create_description),
                    onClick = { onEvent(OnlineLobbyEvent.ChooseCreate) },
                )
                OnlineActionCard(
                    title = stringResource(Res.string.online_join_match),
                    description = stringResource(Res.string.online_join_description),
                    onClick = { onEvent(OnlineLobbyEvent.ChooseJoin) },
                )
                OnlineActionCard(
                    title = stringResource(Res.string.online_browse_match),
                    description = stringResource(Res.string.online_browse_description),
                    onClick = { onEvent(OnlineLobbyEvent.ChooseBrowse) },
                )
            }
            BackButton(onBack)
        }
    }
}

@Composable
private fun OnlineActionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun CreateStep(
    state: OnlineLobbyUiState,
    onEvent: (OnlineLobbyEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_create_match), onBack = onBack) {
        if (state.phase == OnlineLobbyPhase.WaitingForOpponent) {
            WaitingSection(
                code = state.hostedCode.orEmpty(),
                joinedCount = state.joinedCount,
                playerCount = state.playerCount,
                onCancel = { onEvent(OnlineLobbyEvent.Cancel) },
            )
        } else {
            PlayerCountSelector(
                selected = state.playerCount,
                onSelect = { onEvent(OnlineLobbyEvent.PlayerCountChanged(it)) },
            )
            VisibilityToggle(
                isPublic = state.isPublic,
                onToggle = { onEvent(OnlineLobbyEvent.VisibilityChanged(it)) },
            )
            Button(
                onClick = { onEvent(OnlineLobbyEvent.CreateMatch) },
                enabled = state.canCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.phase == OnlineLobbyPhase.Creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.online_create_match),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        ErrorText(state.error)
    }
}

@Composable
private fun JoinStep(
    state: OnlineLobbyUiState,
    onEvent: (OnlineLobbyEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_join_match), onBack = onBack) {
        OutlinedTextField(
            value = state.joinCode,
            onValueChange = { onEvent(OnlineLobbyEvent.JoinCodeChanged(it)) },
            singleLine = true,
            label = {
                Text(
                    text = stringResource(Res.string.online_join_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onEvent(OnlineLobbyEvent.JoinMatch) },
            enabled = state.canJoinByCode,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.phase == OnlineLobbyPhase.Joining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(Res.string.online_join_match),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        ErrorText(state.error)
    }
}

@Composable
private fun PlayerCountSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.online_player_count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            for (count in MIN_PLAYERS..MAX_PLAYERS step (MAX_PLAYERS - MIN_PLAYERS)) {
                PlayerCountOption(
                    count = count,
                    selected = selected == count,
                    onSelect = { onSelect(count) },
                )
            }
        }
    }
}

@Composable
private fun PlayerCountOption(
    count: Int,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun VisibilityToggle(
    isPublic: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.online_public_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.online_public_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isPublic,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun BrowseStep(
    state: OnlineLobbyUiState,
    onEvent: (OnlineLobbyEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(
        title = stringResource(Res.string.online_browse_match),
        onBack = onBack,
        action = {
            IconButton(
                onClick = { onEvent(OnlineLobbyEvent.RefreshBrowse) },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(Res.string.online_browse_refresh),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    ) {
        if (state.openMatches.isEmpty()) {
            if (state.phase == OnlineLobbyPhase.Joining) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = stringResource(Res.string.online_browse_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            for (match in state.openMatches) {
                OpenMatchCard(
                    match = match,
                    enabled = state.canJoin,
                    joining = state.phase == OnlineLobbyPhase.Joining,
                    onJoin = { onEvent(OnlineLobbyEvent.JoinPublicMatch(match.id)) },
                )
            }
        }
        ErrorText(state.error)
    }
}

@Composable
private fun OpenMatchCard(
    match: OnlineOpenMatch,
    enabled: Boolean,
    joining: Boolean,
    onJoin: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(Res.string.online_open_match_host, match.hostName),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    Res.string.online_open_match_players,
                    match.joinedCount,
                    match.playerCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

            if (joining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Button(onClick = onJoin, enabled = enabled) {
                    Text(
                        text = stringResource(Res.string.online_join_match),
                        style = MaterialTheme.typography.titleMedium,
                    )
            }
        }

    }
}

@Composable
private fun WaitingSection(
    code: String,
    joinedCount: Int,
    playerCount: Int,
    onCancel: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.online_share_code),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = { clipboardManager.setText(AnnotatedString(code)) }) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.online_copy_code),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(Res.string.online_waiting_opponent),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.online_waiting_players, joinedCount, playerCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        OutlinedButton(onClick = onCancel) {
            Text(
                text = stringResource(Res.string.online_cancel),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ErrorText(error: OnlineLobbyError?) {
    error ?: return
    Text(
        text = errorText(error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun errorText(error: OnlineLobbyError): String = when (error) {
    OnlineLobbyError.NotFound -> stringResource(Res.string.online_error_not_found)
    OnlineLobbyError.NotJoinable -> stringResource(Res.string.online_error_not_joinable)
    OnlineLobbyError.IncompatibleVersion -> stringResource(Res.string.online_error_incompatible_version)
    OnlineLobbyError.Connection -> stringResource(Res.string.online_error_connection)
    OnlineLobbyError.Unsupported -> stringResource(Res.string.online_error_unsupported)
}

@Preview
@Composable
private fun OnlineNameStepPreview() {
    QuoridorTheme {
        NameStep(state = OnlineLobbyUiState(step = OnlineLobbyStep.Name), onEvent = {}, onBack = {})
    }
}

@Preview
@Composable
private fun OnlineMenuStepPreview() {
    QuoridorTheme {
        MenuStep(onEvent = {}, onBack = {})
    }
}

@Preview
@Composable
private fun OnlineCreateStepPreview() {
    QuoridorTheme {
        CreateStep(
            state = OnlineLobbyUiState(step = OnlineLobbyStep.Create, playerName = "Ana"),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OnlineWaitingStepPreview() {
    QuoridorTheme {
        CreateStep(
            state = OnlineLobbyUiState(
                step = OnlineLobbyStep.Create,
                phase = OnlineLobbyPhase.WaitingForOpponent,
                playerName = "Ana",
                hostedCode = "ABC123",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OnlineJoinStepPreview() {
    QuoridorTheme {
        JoinStep(
            state = OnlineLobbyUiState(step = OnlineLobbyStep.Join, playerName = "Ana"),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OnlineBrowseStepPreview() {
    QuoridorTheme {
        BrowseStep(
            state = OnlineLobbyUiState(
                step = OnlineLobbyStep.Browse,
                playerName = "Ana",
                openMatches = listOf(
                    OnlineOpenMatch(MatchId("ABC123"), hostName = "Beto", joinedCount = 1, playerCount = 2),
                    OnlineOpenMatch(MatchId("XYZ789"), hostName = "Caro", joinedCount = 2, playerCount = 4),
                ),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun OnlineBrowseEmptyStepPreview() {
    QuoridorTheme {
        BrowseStep(
            state = OnlineLobbyUiState(step = OnlineLobbyStep.Browse, playerName = "Ana"),
            onEvent = {},
            onBack = {},
        )
    }
}
