package com.btello.quoridor.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.about_author
import quoridor.app.shared.generated.resources.about_author_role
import quoridor.app.shared.generated.resources.about_github
import quoridor.app.shared.generated.resources.about_github_url
import quoridor.app.shared.generated.resources.about_linkedin
import quoridor.app.shared.generated.resources.about_linkedin_url
import quoridor.app.shared.generated.resources.settings_about
import quoridor.app.shared.generated.resources.settings_dark_mode
import quoridor.app.shared.generated.resources.settings_theme
import quoridor.app.shared.generated.resources.settings_theme_description
import quoridor.app.shared.generated.resources.settings_title

@Composable
internal fun SettingsScreen(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            ThemeSetting(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
            AboutSetting()
        }
    }
}

@Composable
private fun SettingsCard(
    titleRes: StringResource,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun ThemeSetting(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    SettingsCard(titleRes = Res.string.settings_theme) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.settings_theme_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = darkTheme, onCheckedChange = onToggleTheme)
        }
    }
}

@Composable
private fun AboutSetting() {
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(Res.string.about_github_url)
    val linkedinUrl = stringResource(Res.string.about_linkedin_url)

    SettingsCard(titleRes = Res.string.settings_about) {
        Text(
            text = stringResource(Res.string.about_author),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.about_author_role),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinkRow(labelRes = Res.string.about_github, onClick = { uriHandler.openUri(githubUrl) })
        LinkRow(labelRes = Res.string.about_linkedin, onClick = { uriHandler.openUri(linkedinUrl) })
    }
}

@Composable
private fun LinkRow(
    labelRes: StringResource,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    QuoridorTheme {
        SettingsScreen(darkTheme = true, onToggleTheme = {})
    }
}
