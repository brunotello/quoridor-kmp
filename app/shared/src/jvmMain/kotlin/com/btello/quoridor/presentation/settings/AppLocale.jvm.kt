package com.btello.quoridor.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import java.util.Locale

@Composable
actual fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
    if (languageTag != null) {
        Locale.setDefault(Locale.forLanguageTag(languageTag))
    }
    key(languageTag) {
        content()
    }
}
