package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlineMatchSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `online match round-trips through json`() {
        val config = GameConfig(playerCount = 2)
        val start = QuoridorRules.startGame(config)
        val moved = QuoridorRules.applyMove(
            start,
            QuoridorRules.getLegalMoves(start).first(),
        ).state ?: start
        val match = OnlineMatch(
            id = MatchId("ROOM01"),
            config = config,
            status = MatchStatus.IN_PROGRESS,
            state = moved,
            version = 3,
            playerNames = listOf("Ana", "Beto"),
            presence = listOf(true, true),
        )

        val encoded = json.encodeToString(OnlineMatch.serializer(), match)
        val decoded = json.decodeFromString(OnlineMatch.serializer(), encoded)

        assertEquals(match, decoded)
    }

    @Test
    fun `match id serializes as its raw string value`() {
        val encoded = json.encodeToString(MatchId.serializer(), MatchId("ABC123"))
        assertEquals("\"ABC123\"", encoded)
    }
}
