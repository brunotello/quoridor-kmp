package com.btello.quoridor.data.player

/**
 * Persistencia del nombre elegido por el jugador local. Se define como interfaz
 * para respetar la regla de dependencias (la presentación depende de la
 * abstracción) y poder inyectar una implementación falsa en los tests.
 */
internal interface PlayerNameRepository {

    /** Nombre guardado, o cadena vacía si el jugador todavía no lo definió. */
    fun name(): String

    /** Guarda el [name] del jugador local en el almacenamiento de la plataforma. */
    fun setName(name: String)
}
