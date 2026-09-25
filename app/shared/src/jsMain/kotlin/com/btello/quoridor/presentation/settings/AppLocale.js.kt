package com.btello.quoridor.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

// En web el idioma se resuelve desde navigator.languages (solo lectura), por lo
// que no puede forzarse en runtime de forma confiable. Solo se recompone.
@Composable
actual fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
    key(languageTag) {
        content()
    }
}
