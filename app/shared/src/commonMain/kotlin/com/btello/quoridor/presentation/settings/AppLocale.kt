package com.btello.quoridor.presentation.settings

import androidx.compose.runtime.Composable

/**
 * Aplica el idioma [languageTag] (`null` = idioma del sistema) a todo [content]
 * para que Compose Resources resuelva los `stringResource` en ese idioma.
 *
 * Cada plataforma resuelve `androidx.compose.ui.text.intl.Locale.current` de una
 * forma distinta (JVM/Android vía `java.util.Locale`, iOS vía `AppleLanguages`,
 * web vía `navigator.languages`), por lo que la aplicación del locale se
 * implementa como `actual` en cada target. La recomposición se fuerza con `key`
 * para que los recursos se recarguen al instante en el nuevo idioma.
 */
@Composable
expect fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit)
