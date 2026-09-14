package com.btello.quoridor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform