package com.btello.quoridor.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * Tipografía de la app basada en Material 3.
 *
 * Los pesos que la UI necesita se hornean aquí para evitar `fontWeight`/`fontSize`
 * sueltos en los `Text` de las pantallas.
 */
internal val QuoridorTypography: Typography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}
