package com.btello.quoridor.data.player

import com.russhwolf.settings.Settings

/**
 * Localizador simple del [PlayerNameRepository] de producción.
 *
 * El proyecto no usa un framework de inyección de dependencias, por lo que este
 * objeto expone una única instancia perezosa respaldada por el almacenamiento de
 * la plataforma. Los tests no lo usan: inyectan su propia implementación.
 */
internal object PlayerNameProvider {
    val repository: PlayerNameRepository by lazy {
        SettingsPlayerNameRepository(Settings())
    }
}
