package com.btello.quoridor.presentation.game

/**
 * Lógica pura (sin dependencias de Compose) para el zoom/paneo del tablero.
 *
 * Permite acercar el tablero para ubicar muros o mover el peón con mayor
 * precisión. La UI aplica estas transformaciones mediante `graphicsLayer` y las
 * restablece automáticamente al completarse un movimiento.
 */
internal object BoardZoom {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 3f

    /** Escala resultante al aplicar un factor de [zoom], acotada al rango permitido. */
    fun nextScale(current: Float, zoom: Float): Float =
        (current * zoom).coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * Traslación máxima (en px) que mantiene el tablero cubriendo el viewport
     * para una [scale] y un tamaño de tablero [sizePx] dados.
     */
    fun maxTranslation(scale: Float, sizePx: Float): Float =
        ((scale - MIN_SCALE) * sizePx / 2f).coerceAtLeast(0f)

    /** `true` cuando la [scale] supera el nivel base, es decir, el tablero está ampliado. */
    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE

    /** Acota una traslación al desplazamiento máximo permitido según [scale] y [sizePx]. */
    fun clampTranslation(value: Float, scale: Float, sizePx: Float): Float {
        val max = maxTranslation(scale, sizePx)
        return value.coerceIn(-max, max) + 0f
    }
}
