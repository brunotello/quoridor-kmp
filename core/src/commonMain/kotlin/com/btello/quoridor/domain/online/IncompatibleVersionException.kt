package com.btello.quoridor.domain.online

/**
 * Se lanza al intentar unirse a una partida creada con una versión de la app
 * distinta a la local. Ambos jugadores deben compartir versión para garantizar
 * que el estado sincronizado y las reglas sean compatibles.
 */
class IncompatibleVersionException(
    val requiredVersion: String,
    val localVersion: String,
) : Exception(
    "Match requires app version '$requiredVersion' but local version is '$localVersion'",
)
