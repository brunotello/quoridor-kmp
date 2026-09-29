package com.btello.quoridor

import androidx.lifecycle.ViewModel
import com.btello.quoridor.presentation.navigation.newViewModelStoreOwner
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class ScopedViewModelStoreOwnerTest {

    private class TrackingViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun newViewModelStoreOwner_creates_isolated_stores() {
        val first = newViewModelStoreOwner()
        val second = newViewModelStoreOwner()

        assertNotSame(first, second)
        assertNotSame(first.viewModelStore, second.viewModelStore)
    }

    @Test
    fun clearing_store_invokes_onCleared_on_hosted_view_models() {
        val owner = newViewModelStoreOwner()
        val viewModel = TrackingViewModel()
        owner.viewModelStore.put("vm", viewModel)

        owner.viewModelStore.clear()

        assertTrue(viewModel.cleared)
    }
}
