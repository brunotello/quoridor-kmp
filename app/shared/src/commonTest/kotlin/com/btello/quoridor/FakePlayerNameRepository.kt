package com.btello.quoridor

import com.btello.quoridor.data.player.PlayerNameRepository

/** [PlayerNameRepository] en memoria para tests deterministas (sin almacenamiento real). */
internal class FakePlayerNameRepository(
    private var storedName: String = "",
) : PlayerNameRepository {

    override fun name(): String = storedName

    override fun setName(name: String) {
        storedName = name.trim()
    }
}
