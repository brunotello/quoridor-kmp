package com.btello.quoridor.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    containerColor: Color,
) {
    MaterialSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}
