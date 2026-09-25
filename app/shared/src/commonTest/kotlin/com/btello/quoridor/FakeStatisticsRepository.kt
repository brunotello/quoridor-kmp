package com.btello.quoridor

import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.domain.stats.StatisticsRepository

/** Repositorio en memoria para tests deterministas. */
class FakeStatisticsRepository(
    initial: List<GameRecord> = emptyList(),
) : StatisticsRepository {
    private val stored = initial.toMutableList()

    override fun record(record: GameRecord) {
        stored += record
    }

    override fun records(): List<GameRecord> = stored.toList()

    override fun clear() {
        stored.clear()
    }
}
