package com.btello.quoridor.presentation.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Aplica el padding de las áreas seguras superior y laterales (status bar, notch)
 * para que el contenido no quede debajo de la hora ni de los iconos del sistema.
 *
 * No incluye el borde inferior: en las pantallas con bottom navigation ese
 * espacio lo maneja el `Scaffold`, y en las demás el contenido llega hasta abajo.
 */
@Composable
fun Modifier.safeAreaTopPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )
