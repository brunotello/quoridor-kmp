package com.btello.quoridor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/** Crea un [ViewModelStoreOwner] aislado, con su propio [ViewModelStore] vacío. */
internal fun newViewModelStoreOwner(): ViewModelStoreOwner =
    object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }

/**
 * Provee a [content] un [ViewModelStoreOwner] nuevo y aislado, de modo que los
 * ViewModels obtenidos con `viewModel { }` sean instancias frescas ligadas a esta
 * entrada en la composición. Al salir de la composición limpia el store, invocando
 * `onCleared()` en cada ViewModel; así, al reingresar a la pantalla se crea una
 * instancia nueva sin arrastrar el estado anterior.
 */
@Composable
internal fun ScopedViewModelStoreOwner(content: @Composable () -> Unit) {
    val owner = remember { newViewModelStoreOwner() }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}
