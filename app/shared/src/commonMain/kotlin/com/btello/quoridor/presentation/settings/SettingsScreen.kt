package com.btello.quoridor.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.AppConfig
import com.btello.quoridor.data.player.PlayerNameProvider
import com.btello.quoridor.presentation.components.PlatformSwitch
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.app_version_format
import quoridor.app.shared.generated.resources.dialog_cancel
import quoridor.app.shared.generated.resources.dialog_ok
import quoridor.app.shared.generated.resources.edit_player_name
import quoridor.app.shared.generated.resources.settings_about
import quoridor.app.shared.generated.resources.settings_app_version
import quoridor.app.shared.generated.resources.settings_dark_mode
import quoridor.app.shared.generated.resources.settings_language
import quoridor.app.shared.generated.resources.settings_language_english
import quoridor.app.shared.generated.resources.settings_language_spanish
import quoridor.app.shared.generated.resources.settings_player_name
import quoridor.app.shared.generated.resources.settings_player_name_empty
import quoridor.app.shared.generated.resources.settings_player_name_hint
import quoridor.app.shared.generated.resources.settings_section_about
import quoridor.app.shared.generated.resources.settings_section_preferences
import quoridor.app.shared.generated.resources.settings_title
import quoridor.app.shared.generated.resources.select_language

@Composable
internal fun SettingsScreen(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(PlayerNameProvider.repository) },
) {
    SettingsContent(
        state = viewModel.uiState,
        darkTheme = darkTheme,
        onToggleTheme = onToggleTheme,
        language = language,
        onLanguageChange = onLanguageChange,
        onNavigateToAbout = onNavigateToAbout,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onNavigateToAbout: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            )

            SettingsSection(
                title = stringResource(Res.string.settings_section_preferences),
                content = {
                    PlayerNameSetting(
                        playerName = state.playerName,
                        onClick = { onEvent(SettingsEvent.ShowNameDialog) },
                    )
                    DarkModeSetting(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
                    LanguageSetting(
                        selectedLanguage = language,
                        onClick = { onEvent(SettingsEvent.ShowLanguageDialog) },
                    )
                },
            )

            SettingsSection(
                title = stringResource(Res.string.settings_section_about),
                content = {
                    AboutSettingItem(onNavigate = onNavigateToAbout)
                    AppVersionSetting()
                },
            )
        }
    }

    if (state.showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = language,
            onLanguageSelected = {
                onLanguageChange(it)
                onEvent(SettingsEvent.DismissLanguageDialog)
            },
            onDismiss = { onEvent(SettingsEvent.DismissLanguageDialog) },
        )
    }

    if (state.showNameDialog) {
        PlayerNameDialog(
            currentName = state.playerName,
            onConfirm = { newName -> onEvent(SettingsEvent.ConfirmName(newName)) },
            onDismiss = { onEvent(SettingsEvent.DismissNameDialog) },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    titleRes: StringResource,
    descriptionRes: StringResource? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (descriptionRes != null) {
                        Text(
                            text = stringResource(descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun PlayerNameSetting(
    playerName: String,
    onClick: () -> Unit,
) {
    SettingItem(
        icon = Icons.Filled.Person,
        titleRes = Res.string.settings_player_name,
        trailingContent = {
            Text(
                text = playerName.ifBlank { stringResource(Res.string.settings_player_name_empty) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun DarkModeSetting(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    SettingItem(
        icon = Icons.Filled.DarkMode,
        titleRes = Res.string.settings_dark_mode,
        trailingContent = {
            PlatformSwitch(
                checked = darkTheme,
                onCheckedChange = onToggleTheme,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        },
    )
}

@Composable
private fun LanguageSetting(
    selectedLanguage: String,
    onClick: () -> Unit,
) {
    val languageLabel = if (selectedLanguage == LANGUAGE_SPANISH) {
        Res.string.settings_language_spanish
    } else {
        Res.string.settings_language_english
    }
    SettingItem(
        icon = Icons.Filled.Language,
        titleRes = Res.string.settings_language,
        trailingContent = {
            Text(
                text = stringResource(languageLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun AboutSettingItem(onNavigate: () -> Unit) {
    SettingItem(
        icon = Icons.Filled.Info,
        titleRes = Res.string.settings_about,
        onClick = onNavigate,
    )
}

@Composable
private fun AppVersionSetting() {
    SettingItem(
        icon = Icons.Filled.Numbers,
        titleRes = Res.string.settings_app_version,
        trailingContent = {
            Text(
                text = stringResource(Res.string.app_version_format, AppConfig.VERSION),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun PlayerNameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.edit_player_name),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(Res.string.settings_player_name_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun LanguageDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.select_language),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguageOption(
                    label = stringResource(Res.string.settings_language_spanish),
                    isSelected = selectedLanguage == LANGUAGE_SPANISH,
                    onClick = { onLanguageSelected(LANGUAGE_SPANISH) },
                )
                LanguageOption(
                    label = stringResource(Res.string.settings_language_english),
                    isSelected = selectedLanguage == LANGUAGE_ENGLISH,
                    onClick = { onLanguageSelected(LANGUAGE_ENGLISH) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    QuoridorTheme {
        SettingsContent(
            state = SettingsUiState(playerName = "Ana"),
            darkTheme = true,
            onToggleTheme = {},
            language = LANGUAGE_SPANISH,
            onLanguageChange = {},
            onNavigateToAbout = {},
            onEvent = {},
        )
    }
}
