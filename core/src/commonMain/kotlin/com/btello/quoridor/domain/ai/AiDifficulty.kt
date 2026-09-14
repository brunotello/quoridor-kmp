package com.btello.quoridor.domain.ai

/**
 * Niveles de dificultad del oponente controlado por la IA.
 *
 * - [EASY]: principiante, mayormente reactivo y con jugadas aleatorias.
 * - [MEDIUM]: intermedio, avanza por el camino más corto y coloca muros sólo si
 *   mejoran la diferencia de distancias.
 * - [HARD]: avanzado, evalúa la respuesta del rival (minimax acotado a 2 plies).
 */
enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD,
}
