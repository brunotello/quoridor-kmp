package com.btello.quoridor.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.btello.quoridor.presentation.theme.QuoridorTheme

/**
 * Switch con apariencia nativa por plataforma.
 *
 * - Android / Desktop / Web: `Switch` de Material 3.
 * - iOS: `UISwitch` nativo (interop UIKit), para que se vea como el toggle del
 *   sistema.
 *
 * @param containerColor color del contenedor Compose detras del switch. Solo lo
 *   usa iOS: al componer el `UISwitch` nativo, las esquinas transparentes del
 *   control dejarian ver el fondo del interop; pintarlas con este color evita ese
 *   artefacto. En el resto de plataformas se ignora.
 */
@Composable
expect fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
)

/** Implementacion Material reutilizada por los targets no-iOS. */
@Composable
internal fun MaterialSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}

@Preview
@Composable
private fun PlatformSwitchPreview() {
    QuoridorTheme {
        PlatformSwitch(
            checked = true,
            onCheckedChange = {},
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Preview
@Composable
private fun MaterialSwitchPreview() {
    QuoridorTheme {
        MaterialSwitch(checked = false, onCheckedChange = {})
    }
}
