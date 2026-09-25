package com.btello.quoridor.presentation.settings

internal const val LANGUAGE_SPANISH = "es"
internal const val LANGUAGE_ENGLISH = "en"

/**
 * Resuelve el idioma por defecto a partir del código de idioma del dispositivo.
 *
 * Se usa el idioma del dispositivo cuando está soportado; en cualquier otro
 * caso se cae a inglés. Es una función pura para poder testearla sin Compose.
 */
internal fun defaultLanguageCode(deviceLanguage: String): String =
    if (deviceLanguage.trim().lowercase().startsWith(LANGUAGE_SPANISH)) {
        LANGUAGE_SPANISH
    } else {
        LANGUAGE_ENGLISH
    }
