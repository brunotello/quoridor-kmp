package com.btello.quoridor.presentation.online

import com.btello.quoridor.domain.online.IncompatibleVersionException

/** Tiempo máximo (30 s) que se espera a completar la unión a una sala online. */
internal const val ONLINE_JOIN_TIMEOUT_MILLIS: Long = 30_000L

/** Errores mostrables de las pantallas online, resueltos a texto en la capa Compose. */
internal enum class OnlineError {
    NotFound,
    NotJoinable,
    IncompatibleVersion,
    Connection,
    Unsupported,

    /** La unión a una sala no se completó dentro del tiempo límite. */
    JoinTimeout,
}

/** Traduce la excepción de una unión fallida a un [OnlineError] mostrable. */
internal fun Throwable.toOnlineError(): OnlineError = when (this) {
    is NoSuchElementException -> OnlineError.NotFound
    is IncompatibleVersionException -> OnlineError.IncompatibleVersion
    is IllegalStateException -> OnlineError.NotJoinable
    else -> OnlineError.Connection
}
