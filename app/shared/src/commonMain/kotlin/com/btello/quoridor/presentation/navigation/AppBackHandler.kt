package com.btello.quoridor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * Punto único de manejo del gesto/botón "atrás" del sistema en toda la app.
 *
 * Encapsula el efecto de Compose para atrás de modo que las pantallas no dependan
 * directamente de una API marcada como experimental/deprecada ni repartan `@OptIn`
 * por todos lados.
 *
 * ## Por qué no se migra aún a `NavigationEventHandler`
 * La deprecación de `BackHandler` apunta a `androidx.navigationevent.compose.NavigationEventHandler`.
 * En Compose Multiplatform 1.11.1 esa API pública **no está disponible para `commonMain`**:
 * el runtime sólo expone el `NavigationEventDispatcherOwner` mediante un puente interno
 * (`findDefaultNavigationEventDispatcherOwner`, `@InternalComposeApi`) presente únicamente en los
 * source sets *skiko*, mientras que el `LocalNavigationEventDispatcherOwner` público de androidx
 * nunca se provee. Migrar requeriría `expect/actual` por plataforma sobre APIs internas, con riesgo
 * de fallos en runtime. Cuando CMP publique el owner público, basta cambiar la implementación de
 * este wrapper y eliminar la supresión.
 *
 * No es un componente visual (no dibuja nada), por eso no lleva `@Preview`.
 *
 * @param enabled si el manejo de atrás está activo.
 * @param onBack acción a ejecutar cuando se dispara el evento de atrás.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
