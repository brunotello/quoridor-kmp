package com.btello.quoridor.data.online

import com.btello.quoridor.domain.online.OnlineGameRepository

/**
 * Punto de acceso, dependiente de plataforma, al backend online.
 *
 * El SDK de Firebase (GitLive) soporta Android, iOS, JVM (desktop) y JS, pero no
 * `wasmJs`. Por eso [isSupported] es `false` y [repositoryOrNull] devuelve `null`
 * en las plataformas sin soporte, de modo que la UI pueda ocultar/deshabilitar el
 * modo online sin romper la compilación multiplataforma.
 */
expect object OnlinePlatform {
    /** `true` si esta plataforma puede jugar online. */
    val isSupported: Boolean

    /** Repositorio online de la plataforma, o `null` si no está soportado. */
    fun repositoryOrNull(): OnlineGameRepository?
}
