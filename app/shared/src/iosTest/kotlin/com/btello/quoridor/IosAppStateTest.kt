package com.btello.quoridor

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosAppStateTest {

    @AfterTest
    fun tearDown() {
        IosAppState.darkTheme.value = true
    }

    @Test
    fun defaultsToDarkTheme() {
        assertTrue(IosAppState.darkTheme.value)
    }

    @Test
    fun togglesTheme() {
        IosAppState.darkTheme.value = false
        assertFalse(IosAppState.darkTheme.value)

        IosAppState.darkTheme.value = true
        assertEquals(true, IosAppState.darkTheme.value)
    }
}
