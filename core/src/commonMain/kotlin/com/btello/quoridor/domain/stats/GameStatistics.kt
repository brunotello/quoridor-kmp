package com.btello.quoridor.domain.stats

import com.btello.quoridor.domain.ai.AiDifficulty

/**
 * Desglose de partidas por dificultad.
 *
 * @property difficulty dificultad de la IA, o `null` para las partidas locales 1 vs 1.
 * @property wins partidas ganadas por el humano con esa dificultad.
 * @property losses partidas perdidas por el humano con esa dificultad.
 */
data class DifficultyBreakdown(
    val difficulty: AiDifficulty?,
    val wins: Int,
    val losses: Int,
) {
    /** Total de partidas jugadas con esa dificultad. */
    val total: Int get() = wins + losses
}

/**
 * Estadísticas agregadas de todas las partidas registradas. Objeto de dominio
 * puro, calculado a partir de una lista de [GameRecord] con [from].
 *
 * @property totalGames cantidad total de partidas jugadas.
 * @property breakdown desglose de partidas por dificultad (y resultado).
 * @property bestTimeMillis menor duración entre las partidas ganadas, o `null`
 *   si aún no se ganó ninguna.
 * @property fewestMoves menor cantidad de jugadas entre las partidas ganadas, o
 *   `null` si aún no se ganó ninguna.
 * @property fewestWalls menor cantidad de muros usados entre las partidas
 *   ganadas, o `null` si aún no se ganó ninguna.
 */
data class GameStatistics(
    val totalGames: Int,
    val breakdown: List<DifficultyBreakdown>,
    val bestTimeMillis: Long?,
    val fewestMoves: Int?,
    val fewestWalls: Int?,
) {
    companion object {
        /** Estadísticas vacías (sin partidas registradas). */
        val EMPTY = GameStatistics(
            totalGames = 0,
            breakdown = emptyList(),
            bestTimeMillis = null,
            fewestMoves = null,
            fewestWalls = null,
        )

        /**
         * Agrega una lista de [GameRecord] en [GameStatistics]. Es una función
         * pura: mismo input, mismo output.
         *
         * El desglose se ordena por dificultad (según el orden del enum) dejando
         * las partidas locales (`null`) al final. Los "mejores" registros (tiempo,
         * jugadas, muros) se calculan sólo sobre partidas ganadas.
         */
        fun from(records: List<GameRecord>): GameStatistics {
            if (records.isEmpty()) return EMPTY

            val breakdown = records
                .groupBy { it.difficulty }
                .map { (difficulty, group) ->
                    DifficultyBreakdown(
                        difficulty = difficulty,
                        wins = group.count { it.won },
                        losses = group.count { !it.won },
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.difficulty })

            val won = records.filter { it.won }
            return GameStatistics(
                totalGames = records.size,
                breakdown = breakdown,
                bestTimeMillis = won.minOfOrNull { it.durationMillis },
                fewestMoves = won.minOfOrNull { it.moveCount },
                fewestWalls = won.minOfOrNull { it.wallsUsed },
            )
        }
    }
}
