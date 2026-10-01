package com.btello.quoridor

import com.btello.quoridor.presentation.online.TURN_TIME_OPTIONS
import com.btello.quoridor.presentation.online.create.PLAYER_COUNT_OPTIONS
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateMatchOnlineOptionsTest {

    @Test
    fun playerCountOptionsAreTwoAndFour() {
        assertEquals(listOf(2, 4), PLAYER_COUNT_OPTIONS)
    }

    @Test
    fun turnTimeOptionsStartWithNoLimit() {
        assertEquals(listOf(null, 30, 45, 60), TURN_TIME_OPTIONS)
    }
}
