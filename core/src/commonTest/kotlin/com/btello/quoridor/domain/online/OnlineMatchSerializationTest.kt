package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.rules.QuoridorRules
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `public flag round-trips through json`() {
        val config = GameConfig(playerCount = 2)
        val match = OnlineMatch(
            id = MatchId("ROOM01"),
            config = config,
            status = MatchStatus.WAITING,
            state = QuoridorRules.startGame(config),
            version = 0,
            playerNames = listOf("Ana"),
            presence = listOf(true),
            isPublic = true,
        )

        val decoded = json.decodeFromString(
            OnlineMatch.serializer(),
            json.encodeToString(OnlineMatch.serializer(), match),
        )

        assertTrue(decoded.isPublic)
    }

    @Test
    fun `app version round-trips and drives compatibility`() {
        val config = GameConfig(playerCount = 2)
        val match = OnlineMatch(
            id = MatchId("ROOM01"),
            config = config,
            status = MatchStatus.WAITING,
            state = QuoridorRules.startGame(config),
            version = 0,
            playerNames = listOf("Ana"),
            presence = listOf(true),
            isPublic = true,
            appVersion = "2.4.1",
        )

        val decoded = json.decodeFromString(
            OnlineMatch.serializer(),
            json.encodeToString(OnlineMatch.serializer(), match),
        )

        assertEquals("2.4.1", decoded.appVersion)
        assertTrue(decoded.isCompatibleWith("2.4.1"))
        assertFalse(decoded.isCompatibleWith("2.4.0"))
    }

    @Test
    fun `competitive state round-trips through json`() {
        val config = GameConfig(playerCount = 2)
        val match = OnlineMatch(
            id = MatchId("ROOM01"),
            config = config,
            status = MatchStatus.IN_PROGRESS,
            state = QuoridorRules.startGame(config),
            version = 1,
            playerNames = listOf("Ana", "Beto"),
            presence = listOf(true, true),
            competitive = CompetitiveState(
                config = CompetitiveConfig(
                    format = SeriesFormat.FIRST_TO_3,
                    turnTimeSeconds = 45,
                ),
                wins = listOf(1, 0),
                gameIndex = 1,
            ),
        )

        val decoded = json.decodeFromString(
            OnlineMatch.serializer(),
            json.encodeToString(OnlineMatch.serializer(), match),
        )

        assertEquals(match.competitive, decoded.competitive)
        assertEquals(SeriesFormat.FIRST_TO_3, decoded.competitive.config.format)
        assertEquals(listOf(1, 0), decoded.competitive.wins)
    }

    @Test
    fun `is open to public only when public, waiting and not full`() {
        val config = GameConfig(playerCount = 2)
        val waiting = OnlineMatch(
            id = MatchId("ROOM01"),
            config = config,
            status = MatchStatus.WAITING,
            state = QuoridorRules.startGame(config),
            version = 0,
            playerNames = listOf("Ana"),
            presence = listOf(true),
            isPublic = true,
        )
        assertTrue(waiting.isOpenToPublic)
        assertEquals("Ana", waiting.hostName)

        assertFalse(waiting.copy(isPublic = false).isOpenToPublic)
        assertFalse(waiting.copy(status = MatchStatus.IN_PROGRESS).isOpenToPublic)
        assertFalse(
            waiting.copy(playerNames = listOf("Ana", "Beto"), presence = listOf(true, true)).isOpenToPublic,
        )
    }
}
