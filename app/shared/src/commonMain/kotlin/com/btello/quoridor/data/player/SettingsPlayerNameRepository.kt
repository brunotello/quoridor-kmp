package com.btello.quoridor.data.player

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Implementación de [PlayerNameRepository] respaldada por
 * [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings),
 * que persiste en el almacenamiento clave-valor de cada plataforma.
 */
internal class SettingsPlayerNameRepository(
    private val settings: Settings,
) : PlayerNameRepository {

    override fun name(): String = settings[KEY, ""]

    override fun setName(name: String) {
        settings[KEY] = name.trim()
    }

    private companion object {
        const val KEY = "player_name_v1"
    }
}
