package com.btello.quoridor.presentation.online.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.presentation.online.OnlineBackButton
import com.btello.quoridor.presentation.online.OnlineStepScaffold
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.online_browse_description
import quoridor.app.shared.generated.resources.online_browse_match
import quoridor.app.shared.generated.resources.online_continue
import quoridor.app.shared.generated.resources.online_create_description
import quoridor.app.shared.generated.resources.online_create_match
import quoridor.app.shared.generated.resources.online_join_description
import quoridor.app.shared.generated.resources.online_join_match
import quoridor.app.shared.generated.resources.online_menu_headline
import quoridor.app.shared.generated.resources.online_name_headline
import quoridor.app.shared.generated.resources.online_your_name

/**
 * Pantalla online principal: primero pide el nombre del jugador (si aún no lo
 * definió) y luego muestra las tres acciones disponibles. Cada acción redirige a
 * su propia pantalla mediante los callbacks [onCreate], [onJoin] y [onFind].
 */
@Composable
internal fun OnlineMenuScreen(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onFind: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnlineMenuViewModel = viewModel { OnlineMenuViewModel() },
) {
    val state = viewModel.uiState
    if (state.nameConfirmed) {
        MenuContent(onCreate = onCreate, onJoin = onJoin, onFind = onFind, onBack = onBack)
    } else {
        NameContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
    }
}

@Composable
private fun NameContent(
    state: OnlineMenuUiState,
    onEvent: (OnlineMenuEvent) -> Unit,
    onBack: () -> Unit,
) {
    OnlineStepScaffold(title = stringResource(Res.string.online_name_headline), onBack = onBack) {
        OutlinedTextField(
            value = state.playerName,
            onValueChange = { onEvent(OnlineMenuEvent.NameChanged(it)) },
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
            onClick = { onEvent(OnlineMenuEvent.ConfirmName) },
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
private fun MenuContent(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onFind: () -> Unit,
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
                    onClick = onCreate,
                )
                OnlineActionCard(
                    title = stringResource(Res.string.online_join_match),
                    description = stringResource(Res.string.online_join_description),
                    onClick = onJoin,
                )
                OnlineActionCard(
                    title = stringResource(Res.string.online_browse_match),
                    description = stringResource(Res.string.online_browse_description),
                    onClick = onFind,
                )
            }
            OnlineBackButton(onBack)
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

@Preview
@Composable
private fun OnlineNameContentPreview() {
    QuoridorTheme {
        NameContent(state = OnlineMenuUiState(), onEvent = {}, onBack = {})
    }
}

@Preview
@Composable
private fun OnlineMenuContentPreview() {
    QuoridorTheme {
        MenuContent(onCreate = {}, onJoin = {}, onFind = {}, onBack = {})
    }
}
