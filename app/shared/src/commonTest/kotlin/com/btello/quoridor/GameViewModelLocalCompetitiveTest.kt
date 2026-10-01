package com.btello.quoridor

import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.PlayerId
import com.btello.quoridor.domain.online.CompetitiveConfig
import com.btello.quoridor.domain.online.SeriesFormat
import com.btello.quoridor.presentation.game.GameEvent
import com.btello.quoridor.presentation.game.GameSetup
import com.btello.quoridor.presentation.game.GameViewModel
import com.btello.quoridor.presentation.game.TurnBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(ExperimentalTime::class)
class GameViewModelLocalCompetitiveTest {

    private val config = GameConfig(playerCount = 2)

    @Test
    fun `a plain local game is not competitive`() {
        val vm = GameViewModel(GameSetup(config), autoRunAi = false)
        assertNull(vm.uiState.competitive)
        assertTrue(vm.uiState.isSeriesOver)
    }

    @Test
    fun `versus ai exposes the chosen difficulty`() {
        val vm = GameViewModel(
            GameSetup(config, aiPlayers = setOf(PlayerId(1)), difficulty = AiDifficulty.HARD),
            autoRunAi = false,
        )
        assertEquals(AiDifficulty.HARD, vm.uiState.difficulty)
    }

    @Test
    fun `an all human local game has no difficulty`() {
        val vm = GameViewModel(GameSetup(config), autoRunAi = false)
        assertNull(vm.uiState.difficulty)
    }

    @Test
    fun `winning a round in a local series keeps the series open and counts the win`() {
        val vm = GameViewModel(
            GameSetup(config, competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3)),
            autoRunAi = false,
        )
        vm.endGameForTest(PlayerId(0))

        assertTrue(vm.uiState.isGameOver)
        assertFalse(vm.uiState.isSeriesOver)
        assertEquals(listOf(1, 0), vm.uiState.competitive?.wins)
    }

    @Test
    fun `reaching the required wins ends the local series`() {
        val vm = GameViewModel(
            GameSetup(config, competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3)),
            autoRunAi = false,
        )
        repeat(3) {
            vm.endGameForTest(PlayerId(0))
            if (!vm.uiState.isSeriesOver) vm.onEvent(GameEvent.ContinueSeries)
        }

        assertTrue(vm.uiState.isGameOver)
        assertTrue(vm.uiState.isSeriesOver)
        assertEquals(3, vm.uiState.competitive?.wins?.get(0))
    }

    @Test
    fun `continuing the series resets the board but keeps the scoreboard`() {
        val vm = GameViewModel(
            GameSetup(config, competitive = CompetitiveConfig(format = SeriesFormat.FIRST_TO_3)),
            autoRunAi = false,
        )
        vm.endGameForTest(PlayerId(0))
        vm.onEvent(GameEvent.ContinueSeries)

        assertFalse(vm.uiState.isGameOver)
        assertNull(vm.uiState.gameState.winner)
        assertEquals(listOf(1, 0), vm.uiState.competitive?.wins)
    }

    @Test
    fun `turn banner says your turn for the single human and names the ai turn`() {
        val vm = GameViewModel(
            GameSetup(config, aiPlayers = setOf(PlayerId(1))),
            autoRunAi = false,
        )
        // El humano (jugador 0) empieza o no según el azar; comprobamos ambos casos.
        when (vm.uiState.gameState.turn.playerId) {
            PlayerId(0) -> assertEquals(TurnBanner.YourTurn, vm.uiState.turnBanner)
            else -> {
                val banner = assertIs<TurnBanner.PlayerTurn>(vm.uiState.turnBanner)
                assertEquals(2, banner.playerNumber)
            }
        }
    }

    @Test
    fun `all human local game shows the acting player's turn`() {
        val vm = GameViewModel(GameSetup(config), autoRunAi = false)
        val banner = assertIs<TurnBanner.PlayerTurn>(vm.uiState.turnBanner)
        assertEquals(vm.uiState.gameState.turn.playerId.value + 1, banner.playerNumber)
    }

    @Test
    fun `running out of turn time passes the turn without losing the game`() = runTest {
        val timeSource = TestTimeSource()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = GameViewModel(
            GameSetup(config, competitive = CompetitiveConfig(turnTimeSeconds = 30)),
            autoRunAi = false,
            timeSource = timeSource,
            clockScope = scope,
        )
        val before = vm.uiState.gameState
        timeSource += 31.seconds
        advanceTimeBy(300)
        runCurrent()

        assertFalse(vm.uiState.isGameOver)
        assertEquals(PlayerId(1), vm.uiState.gameState.turn.playerId)
        assertEquals(before.players, vm.uiState.gameState.players)
        scope.cancel()
    }

    @Test
    fun `turn time does not expire before the limit`() = runTest {
        val timeSource = TestTimeSource()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = GameViewModel(
            GameSetup(config, competitive = CompetitiveConfig(turnTimeSeconds = 45)),
            autoRunAi = false,
            timeSource = timeSource,
            clockScope = scope,
        )
        timeSource += 44.seconds
        advanceTimeBy(300)
        runCurrent()

        assertFalse(vm.uiState.isGameOver)
        assertEquals(1_000L, vm.uiState.competitive?.turnRemainingMillis)
        scope.cancel()
    }
}
