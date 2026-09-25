package com.btello.quoridor

import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.presentation.game.TurnBanner
import com.btello.quoridor.presentation.game.turnBannerPlayerId
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnBannerPlayerIdTest {

    @Test
    fun yourTurnUsesLocalPlayerId() {
        assertEquals(1, turnBannerPlayerId(TurnBanner.YourTurn, PlayerId(1)))
    }

    @Test
    fun yourTurnFallsBackToFirstPlayerWhenLocalUnknown() {
        assertEquals(0, turnBannerPlayerId(TurnBanner.YourTurn, null))
    }

    @Test
    fun playerTurnConvertsOneBasedNumberToZeroBasedId() {
        assertEquals(0, turnBannerPlayerId(TurnBanner.PlayerTurn(1, null), null))
        assertEquals(3, turnBannerPlayerId(TurnBanner.PlayerTurn(4, "Ana"), PlayerId(0)))
    }
}
