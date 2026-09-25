package com.btello.quoridor.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import platform.Foundation.NSUserDefaults

private const val LANGUAGES_KEY = "AppleLanguages"

@Composable
actual fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
    if (languageTag != null) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(languageTag), LANGUAGES_KEY)
    }
    key(languageTag) {
        content()
    }
}
