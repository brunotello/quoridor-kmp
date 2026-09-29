package com.btello.quoridor.presentation.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.difficulty_back
import quoridor.app.shared.generated.resources.online_error_connection
import quoridor.app.shared.generated.resources.online_error_incompatible_version
import quoridor.app.shared.generated.resources.online_error_join_timeout
import quoridor.app.shared.generated.resources.online_error_not_found
import quoridor.app.shared.generated.resources.online_error_not_joinable
import quoridor.app.shared.generated.resources.online_error_unsupported

/**
 * Andamiaje común de las pantallas online: fondo, título, botón de volver y un
 * [content] centrado. Reutiliza el mismo estilo que el resto de la aplicación.
 */
@Composable
internal fun OnlineStepScaffold(
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
            OnlineBackButton(onBack)
            action?.invoke(this)
        }
    }
}

@Composable
internal fun BoxScope.OnlineBackButton(onBack: () -> Unit) {
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
internal fun OnlineErrorText(error: OnlineError?) {
    error ?: return
    Text(
        text = onlineErrorText(error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun onlineErrorText(error: OnlineError): String = when (error) {
    OnlineError.NotFound -> stringResource(Res.string.online_error_not_found)
    OnlineError.NotJoinable -> stringResource(Res.string.online_error_not_joinable)
    OnlineError.IncompatibleVersion -> stringResource(Res.string.online_error_incompatible_version)
    OnlineError.Connection -> stringResource(Res.string.online_error_connection)
    OnlineError.Unsupported -> stringResource(Res.string.online_error_unsupported)
    OnlineError.JoinTimeout -> stringResource(Res.string.online_error_join_timeout)
}
