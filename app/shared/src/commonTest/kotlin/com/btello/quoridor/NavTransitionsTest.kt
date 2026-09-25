package com.btello.quoridor

import com.btello.quoridor.presentation.navigation.navSlideDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class NavTransitionsTest {

    @Test
    fun forwardWhenGoingDeeper() {
        assertEquals(1, navSlideDirection(fromDepth = 0, toDepth = 1))
    }

    @Test
    fun backwardWhenGoingShallower() {
        assertEquals(-1, navSlideDirection(fromDepth = 1, toDepth = 0))
    }

    @Test
    fun forwardWhenSameDepth() {
        assertEquals(1, navSlideDirection(fromDepth = 2, toDepth = 2))
    }
}
