package com.btello.quoridor.data.stats

import com.btello.quoridor.domain.stats.StatisticsRepository
import com.russhwolf.settings.Settings

/**
 * Localizador simple del [StatisticsRepository] de producción.
 *
 * El proyecto no usa un framework de inyección de dependencias, por lo que este
 * objeto expone una única instancia perezosa respaldada por el almacenamiento de
 * la plataforma. Los tests no lo usan: inyectan su propia implementación.
 */
internal object StatisticsProvider {
    val repository: StatisticsRepository by lazy {
        SettingsStatisticsRepository(Settings())
    }
}
