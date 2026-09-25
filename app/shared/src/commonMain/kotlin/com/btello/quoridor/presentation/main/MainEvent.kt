package com.btello.quoridor.presentation.main

/**
 * Intenciones del usuario en la pantalla "Nueva partida" (feature `menu`).
 */
internal sealed interface MainEvent {
    data class SelectMode(val mode: GameMode) : MainEvent
    data class SelectPlayerSetup(val mode: GameMode, val option: PlayerSetupOption) : MainEvent
    data class SelectDifficulty(
        val mode: GameMode,
        val aiCount: Int,
        val option: DifficultyOption,
    ) : MainEvent
}
