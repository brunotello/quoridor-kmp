package com.btello.quoridor.data.online

import com.btello.quoridor.domain.online.OnlineGameRepository

/**
 * Implementación de [OnlinePlatform] para las plataformas soportadas por el SDK
 * de Firebase (GitLive): Android, iOS, JVM y JS.
 *
 * El repositorio es perezoso: sólo se construye al pedirlo, y su uso requiere que
 * Firebase esté inicializado en el arranque de cada plataforma (ver README).
 */
actual object OnlinePlatform {
    actual val isSupported: Boolean = true

    private val repository: OnlineGameRepository by lazy { FirebaseOnlineGameRepository() }

    actual fun repositoryOrNull(): OnlineGameRepository? = repository
}
