package com.btello.quoridor.data.online

import com.btello.quoridor.domain.online.OnlineGameRepository

/**
 * Implementación de [OnlinePlatform] para `wasmJs`, plataforma sin soporte del
 * SDK de Firebase (GitLive). El modo online queda deshabilitado.
 */
actual object OnlinePlatform {
    actual val isSupported: Boolean = false

    actual fun repositoryOrNull(): OnlineGameRepository? = null
}
