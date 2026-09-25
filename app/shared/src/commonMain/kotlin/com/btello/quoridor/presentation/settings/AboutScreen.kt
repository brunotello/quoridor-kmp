package com.btello.quoridor.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.action_back
import quoridor.app.shared.generated.resources.about_author
import quoridor.app.shared.generated.resources.about_description
import quoridor.app.shared.generated.resources.about_github
import quoridor.app.shared.generated.resources.about_github_url
import quoridor.app.shared.generated.resources.about_linkedin
import quoridor.app.shared.generated.resources.about_linkedin_url
import quoridor.app.shared.generated.resources.about_source_code
import quoridor.app.shared.generated.resources.ic_github
import quoridor.app.shared.generated.resources.ic_linkedin
import quoridor.app.shared.generated.resources.settings_about

@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(Res.string.about_github_url)
    val linkedinUrl = stringResource(Res.string.about_linkedin_url)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeAreaTopPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
                Text(
                    text = stringResource(Res.string.settings_about),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.about_author),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                Text(
                    text = stringResource(Res.string.about_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(Res.string.about_source_code),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinkItem(
                        iconRes = Res.drawable.ic_github,
                        labelRes = Res.string.about_github,
                        onClick = { uriHandler.openUri(githubUrl) },
                    )

                    LinkItem(
                        iconRes = Res.drawable.ic_linkedin,
                        labelRes = Res.string.about_linkedin,
                        onClick = { uriHandler.openUri(linkedinUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkItem(
    iconRes: DrawableResource,
    labelRes: StringResource,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = vectorResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    QuoridorTheme {
        AboutScreen(onBack = {})
    }
}
