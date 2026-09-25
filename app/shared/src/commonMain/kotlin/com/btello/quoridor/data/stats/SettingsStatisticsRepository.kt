package com.btello.quoridor.data.stats

import com.btello.quoridor.domain.stats.GameRecord
import com.btello.quoridor.domain.stats.StatisticsRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json

/**
 * Implementación de [StatisticsRepository] respaldada por
 * [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings),
 * que persiste en el almacenamiento clave-valor de cada plataforma.
 *
 * Los registros se serializan como una lista JSON bajo una única clave. Ante un
 * JSON corrupto o de una versión incompatible se devuelve una lista vacía en
 * lugar de fallar.
 */
internal class SettingsStatisticsRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : StatisticsRepository {

    override fun records(): List<GameRecord> {
        val raw: String = settings[KEY, ""]
        if (raw.isEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<GameRecord>>(raw) }.getOrDefault(emptyList())
    }

    override fun record(record: GameRecord) {
        val updated = records() + record
        settings[KEY] = json.encodeToString(updated)
    }

    override fun clear() {
        settings.remove(KEY)
    }

    private companion object {
        const val KEY = "game_records_v1"
    }
}
