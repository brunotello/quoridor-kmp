package com.btello.quoridor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.intl.Locale
import com.btello.quoridor.presentation.settings.defaultLanguageCode

/**
 * Estado de app compartido entre los distintos [androidx.compose.ui.window.ComposeUIViewController]
 * que hospeda el `TabView` nativo de iOS.
 *
 * Cada pestaña Liquid Glass es una composición Compose independiente; al usar
 * snapshot state global, un cambio aquí (por ejemplo, el toggle de tema en Ajustes)
 * recompone todas las pestañas y mantiene el tema sincronizado en toda la app.
 */
internal object IosAppState {
    /** `true` = tema oscuro. Coincide con el valor inicial de `App()`. */
    val darkTheme = mutableStateOf(true)

    /** Idioma de la app. Coincide con el valor inicial de `App()`. */
    val language = mutableStateOf(defaultLanguageCode(Locale.current.language))
}
