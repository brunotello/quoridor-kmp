package com.btello.quoridor.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Duración estándar de Material Motion para transiciones de contenedor. */
private const val NAV_ANIMATION_DURATION_MILLIS = 300

/** Duración del tramo de salida (fade out) según Material Motion. */
private const val NAV_OUTGOING_DURATION_MILLIS = 90

/** Duración del tramo de entrada (fade in), tras el retardo inicial. */
private const val NAV_INCOMING_DURATION_MILLIS =
    NAV_ANIMATION_DURATION_MILLIS - NAV_OUTGOING_DURATION_MILLIS

/** Desplazamiento del eje compartido (Material recomienda 30dp). */
private val NAV_SHARED_AXIS_OFFSET = 30.dp

/** Escala inicial del contenido entrante en Fade Through. */
private const val NAV_FADE_THROUGH_INITIAL_SCALE = 0.92f

private val StandardEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val IncomingEasing: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
private val OutgoingEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/**
 * Dirección del deslizamiento entre dos profundidades de navegación:
 * `1` al avanzar (destino igual o más profundo) y `-1` al retroceder.
 */
internal fun navSlideDirection(fromDepth: Int, toDepth: Int): Int =
    if (toDepth < fromDepth) -1 else 1

/**
 * Contenedor animado con el patrón **Shared Axis (X)** de Material Motion, pensado
 * para navegación jerárquica (avanzar/retroceder dentro de un flujo).
 *
 * La dirección depende de la profundidad de cada destino ([depthOf]): avanzar entra
 * desde la derecha y retroceder desde la izquierda, combinando deslizamiento y fade.
 */
@Composable
internal fun <T> NavAnimatedContent(
    targetState: T,
    depthOf: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val offsetPx = with(LocalDensity.current) { NAV_SHARED_AXIS_OFFSET.roundToPx() }
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val direction = navSlideDirection(depthOf(initialState), depthOf(targetState))
            sharedAxisEnter(direction, offsetPx) togetherWith sharedAxisExit(direction, offsetPx)
        },
        label = "NavSharedAxis",
    ) { state ->
        content(state)
    }
}

/**
 * Contenedor animado con el patrón **Fade Through** de Material Motion, recomendado
 * para alternar entre destinos de nivel superior (p. ej. las pestañas de un
 * bottom navigation), donde no hay relación jerárquica ni dirección espacial.
 */
@Composable
internal fun <T> NavFadeThroughContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { fadeThroughEnter() togetherWith fadeThroughExit() },
        label = "NavFadeThrough",
    ) { state ->
        content(state)
    }
}

private fun sharedAxisEnter(direction: Int, offsetPx: Int): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(NAV_ANIMATION_DURATION_MILLIS, easing = StandardEasing),
    ) { direction * offsetPx } +
        fadeIn(
            animationSpec = tween(
                durationMillis = NAV_INCOMING_DURATION_MILLIS,
                delayMillis = NAV_OUTGOING_DURATION_MILLIS,
                easing = IncomingEasing,
            ),
        )

private fun sharedAxisExit(direction: Int, offsetPx: Int): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIMATION_DURATION_MILLIS, easing = StandardEasing),
    ) { -direction * offsetPx } +
        fadeOut(
            animationSpec = tween(
                durationMillis = NAV_OUTGOING_DURATION_MILLIS,
                easing = OutgoingEasing,
            ),
        )

private fun fadeThroughEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = NAV_INCOMING_DURATION_MILLIS,
            delayMillis = NAV_OUTGOING_DURATION_MILLIS,
            easing = IncomingEasing,
        ),
    ) + scaleIn(
        animationSpec = tween(
            durationMillis = NAV_INCOMING_DURATION_MILLIS,
            delayMillis = NAV_OUTGOING_DURATION_MILLIS,
            easing = IncomingEasing,
        ),
        initialScale = NAV_FADE_THROUGH_INITIAL_SCALE,
    )

private fun fadeThroughExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = NAV_OUTGOING_DURATION_MILLIS,
            easing = OutgoingEasing,
        ),
    )
