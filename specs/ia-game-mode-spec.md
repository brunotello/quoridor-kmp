# Quoridor IA Game Mode Spec (modo "Jugador vs IA")

## 1) Objetivo
Definir e implementar el **modo de juego contra la IA** (Jugador vs Máquina) para
partidas locales de 2 jugadores. El humano juega como `PlayerId(0)` y la IA como
`PlayerId(1)`. La lógica de decisión de la IA vive en `core` (dominio),
totalmente desacoplada de la UI, y la capa de presentación se limita a orquestar
los turnos.

## 2) Alcance (v0)
Incluye:
- Habilitar el modo `VERSUS_AI` en la pantalla "Nueva partida" (`GameMode`).
- Un motor de IA en `core` que, dado un `GameState`, devuelve una jugada legal
  (`Move`) para el jugador en turno.
- **Tres niveles de dificultad** (`EASY`, `MEDIUM`, `HARD`), cada uno con una
  estrategia distinta y jugablemente diferenciada.
- **Submenú de selección de dificultad**: al elegir el modo IA en "Nueva
  partida", en lugar de arrancar directamente la partida se navega a una pantalla
  de selección de dificultad; recién al elegir un nivel comienza la partida.
- Orquestación de turnos en la capa de presentación: tras la jugada del humano,
  la IA responde automáticamente.
- Bloqueo de la interacción del humano mientras la IA "piensa" / mueve.
- Tests unitarios del motor de IA (por nivel) y de la orquestación de turnos.

No incluye (aún):
- IA de nivel competitivo con búsqueda profunda (minimax con poda alfa-beta a
  gran profundidad, aprendizaje). `HARD` usa una heurística fuerte pero acotada.
- Multiplayer online.

> Actualización: el modo **4 jugadores** ya está soportado. Desde "Nueva
> partida" el modo `FOUR_PLAYERS` abre un submenú donde se elige cuántos de los 4
> peones controla la IA (0 a 3); el humano principal es siempre `PlayerId(0)` y la
> IA toma los últimos identificadores. Si hay al menos una IA se elige una única
> dificultad para todas. Para que colocar muros tenga valor con varios rivales,
> `positionalScore` suma la distancia de **todos** los oponentes menos la propia
> (en 2 jugadores hay un único rival, por lo que equivale a `dRival - dSelf` y el
> comportamiento no cambia), de modo que el mismo contrato de `AiStrategy` aplica
> a 2 y 4 jugadores.

## 3) Reglas y comportamiento del modo
- La IA sólo controla a los jugadores marcados como controlados por máquina. En
  v0 (2 jugadores): humano = `PlayerId(0)`, IA = `PlayerId(1)`.
- La IA **nunca** produce jugadas ilegales: toda decisión se elige de entre
  `QuoridorRules.getLegalMoves(state)` (o se valida contra `validateMove` antes de
  aplicarla).
- La IA respeta las mismas reglas que un humano: un movimiento **o** un muro por
  turno, límites de muros, saltos, caminos garantizados, etc. (ver
  `rules-spec.md`).
- Si la partida termina (`GameStatus.GAME_OVER`), la IA no juega.
- Determinismo controlable: la fuente de aleatoriedad de la IA (desempates) debe
  ser inyectable (semilla) para poder testear de forma reproducible.

## 4) Niveles de dificultad de la IA
Los tres niveles comparten el mismo contrato (`AiStrategy`) y la misma garantía
de legalidad (toda jugada sale de `getLegalMoves` / se valida contra reglas).
Se diferencian por su heurística de decisión.

Base común — **distancia BFS**: longitud del camino más corto de un jugador a su
`goalSide` sobre el tablero actual (reutilizando el BFS de `QuoridorRules`). Sea
`dSelf` la distancia de la IA y `dRival` la del rival.

### 4.1 `EASY` — principiante
- Comportamiento mayormente reactivo y algo aleatorio para ser "batible".
- Con probabilidad alta, **mueve** el peón por (o hacia) su camino más corto.
- Rara vez coloca muros; si lo hace, de forma casi aleatoria entre los muros
  legales.
- Muchos desempates resueltos al azar (semilla inyectable).

### 4.2 `MEDIUM` — intermedio ("greedy" por camino más corto)
- **Mover peón** siguiendo el primer paso del camino más corto propio, salvo que
  colocar un muro mejore claramente la posición relativa.
- **Colocar muro** sólo si le quedan muros y existe un muro legal que aumente
  `dRival` **más** de lo que aumenta `dSelf` (heurística de diferencia de
  distancias `dRival - dSelf`). Si ningún muro mejora esa diferencia, avanza.
- Desempates pseudoaleatorios con semilla inyectada.

### 4.3 `HARD` — avanzado (búsqueda con evaluación)
- Evalúa jugadas con una función de puntuación basada en `dRival - dSelf`
  (más un pequeño peso por muros restantes y por progreso hacia la meta).
- Realiza una búsqueda acotada (p. ej. minimax de 1–2 plies con poda simple, o
  evaluación de todas las jugadas legales a 1 ply eligiendo la de mejor score),
  considerando tanto movimientos como los muros legales más prometedores
  (candidatos filtrados para acotar el coste).
- Coloca muros de forma estratégica para maximizar `dRival - dSelf`, evitando
  gastar muros sin ganancia.
- Aleatoriedad mínima, sólo para desempates estrictos (comportamiento casi
  determinista con semilla fija).

### 4.4 Reglas comunes
- Ninguna estrategia produce jugadas ilegales.
- Si la partida terminó (`GAME_OVER`) la IA no juega.
- Fallback ante ausencia de "buena" jugada: elegir cualquier `Move.PawnMove`
  legal que no aumente `dSelf`; en último caso, la primera jugada legal.
- La fuente de aleatoriedad (`kotlin.random.Random`) es inyectable para tests
  reproducibles.

> Reutilizar el BFS ya disponible en `QuoridorRules` para validar caminos; si se
> necesita la distancia de forma directa, exponer un helper de dominio
> (`shortestPathLength`) en lugar de duplicar el algoritmo.

## 5) Arquitectura por capas

### 5.1 Dominio (`core`, paquete `com.btello.quoridor`)
- Nuevo paquete `domain/ai` con:
  - `AiStrategy` (interfaz): `fun chooseMove(state: GameState, playerId: PlayerId): Move?`
    - Devuelve `null` sólo si no hay jugada legal (partida terminada o jugador sin
      movimientos, situación que no debería ocurrir en un estado válido).
  - `AiDifficulty` (enum): `EASY`, `MEDIUM`, `HARD` (los tres implementados).
  - Una implementación de `AiStrategy` por nivel (p. ej. `EasyAiStrategy`,
    `MediumAiStrategy`, `HardAiStrategy`), o una estrategia parametrizada por
    `AiDifficulty`, con aleatoriedad inyectable (`kotlin.random.Random`, por
    defecto `Random.Default`).
  - Un factory de dominio que mapea `AiDifficulty -> AiStrategy` (p. ej.
    `AiStrategy.forDifficulty(difficulty, random)`).
  - Opcional: `AiPlayer`/`AiController` que asocia `PlayerId` + `AiStrategy` para
    resolver "de quién es el turno y qué jugada le corresponde".
- Extensión de la API pública en `QuoridorApi.kt`:
  - `fun chooseAiMove(state: GameState, playerId: PlayerId, difficulty: AiDifficulty, random: Random = Random.Default): Move?`
    delegando en la estrategia correspondiente al nivel.
- Si se necesita distancia BFS reutilizable, exponer un helper de dominio (p. ej.
  `QuoridorRules.shortestPathLength(state, playerId): Int?`) en lugar de duplicar
  el BFS en `domain/ai`.
- **Sin dependencias de UI ni de corrutinas** en `domain/ai`: la elección de
  jugada es una función pura (dado `Random`).

### 5.2 Presentación (`app/shared`, paquete `com.btello.quoridor.presentation`)
- `GameMode.VERSUS_AI` pasa a `enabled = true`.
- **Selección de modo en dos pasos** en el feature `main`/`menu`:
  - Al seleccionar `VERSUS_AI`, en vez de emitir directamente
    `NavigateToGame`, se navega a un **submenú de dificultad**
    (`DifficultyScreen` / `DifficultySelectionScreen`). El modo `LOCAL_1V1`
    sigue arrancando la partida directamente.
  - El submenú lista los tres niveles (`EASY`, `MEDIUM`, `HARD`) con título y
    descripción; al elegir uno se emite el evento que arranca la partida con la
    dificultad seleccionada. Debe existir forma de volver atrás sin iniciar
    partida.
- El `GameConfig` (dominio) no cambia; el "quién es IA" y la dificultad viven en
  presentación. Opciones:
  - **Preferida**: introducir en presentación un modelo `GameSetup`/`GameSessionConfig`
    que envuelva `GameConfig` + `aiPlayers: Set<PlayerId>` + `difficulty: AiDifficulty`,
    para no contaminar el dominio con el concepto "quién es IA".
  - La navegación (`App.kt` / `Destination.Game`) transporta ese `GameSetup` en
    lugar de sólo `GameConfig`.
- `GameViewModel`:
  - Recibe qué jugadores son IA y la `AiDifficulty` seleccionada.
  - Tras aplicar una jugada del humano con éxito y si el nuevo jugador en turno es
    IA y la partida no terminó, dispara la jugada de la IA en `viewModelScope`
    (corrutina), con un pequeño retardo (p. ej. 300–600 ms) para que el usuario
    perciba el movimiento.
  - Usa `chooseAiMove(state, playerId, difficulty, random)` para decidir.
  - Mientras la IA decide/mueve, el `GameUiState` expone un flag (p. ej.
    `isAiThinking`/`interactionEnabled = false`) para **bloquear** la entrada del
    humano (celdas, reserva de muros, peón).
  - Encadena varios turnos de IA si hubiera más de un jugador IA consecutivo
    (relevante para el futuro 4 jugadores).
  - Al reiniciar / nueva partida, cancela cualquier turno de IA pendiente.
- `GameScreen`/`BoardView`: respetan el flag de interacción (no procesan
  `GameEvent` de entrada del humano mientras la IA actúa). El feedback puede
  mostrar un indicador de "La IA está pensando…" (texto en `strings.xml`).

## 6) Contrato de dominio (a implementar)
```kotlin
// domain/ai
enum class AiDifficulty { EASY, MEDIUM, HARD }

interface AiStrategy {
    fun chooseMove(state: GameState, playerId: PlayerId): Move?

    companion object {
        fun forDifficulty(
            difficulty: AiDifficulty,
            random: Random = Random.Default,
        ): AiStrategy = when (difficulty) {
            AiDifficulty.EASY -> EasyAiStrategy(random)
            AiDifficulty.MEDIUM -> MediumAiStrategy(random)
            AiDifficulty.HARD -> HardAiStrategy(random)
        }
    }
}

// QuoridorApi.kt
fun chooseAiMove(
    state: GameState,
    playerId: PlayerId,
    difficulty: AiDifficulty,
    random: Random = Random.Default,
): Move?
```

## 7) Flujo de una partida vs IA
1. El usuario elige `VERSUS_AI` en "Nueva partida" → se navega al **submenú de
   dificultad**.
2. El usuario elige un nivel (`EASY`/`MEDIUM`/`HARD`) → se crea la partida con
   humano `PlayerId(0)`, IA `PlayerId(1)` y la dificultad elegida. (Puede volver
   atrás sin iniciar.)
3. Turno del humano: interacción normal (mover / muro). Al aplicar la jugada:
   - Si `GAME_OVER` → pantalla de resultado.
   - Si no y el turno pasa a la IA → bloquear interacción, `isAiThinking = true`.
4. La IA calcula su jugada (`chooseAiMove` con la dificultad de la partida), se
   aplica con `QuoridorRules.applyMove`, se actualiza el estado y se libera la
   interacción (`isAiThinking = false`).
5. Se repite hasta `GAME_OVER`.

## 8) UI / textos
- Reutilizar `game_mode_pvai` / `game_mode_pvai_description` (ya existen) y marcar
  el modo como disponible.
- **Submenú de dificultad**: nueva pantalla (`@Composable` + su `@Preview`) que
  lista los tres niveles. Modelar los niveles de UI análogamente a `GameMode`
  (título, descripción, icono por nivel) para que agregar/ajustar un nivel sea
  declarativo.
- Nuevos textos en `*/src/commonMain/composeResources/values/strings.xml`
  (consumidos con `stringResource`, sin literales en Kotlin):
  - Título de la pantalla de dificultad (p. ej. `difficulty_title`).
  - Título y descripción de cada nivel: `difficulty_easy`,
    `difficulty_easy_description`, `difficulty_medium`,
    `difficulty_medium_description`, `difficulty_hard`,
    `difficulty_hard_description`.
  - Indicador de turno de IA (p. ej. `ai_thinking`).
  - Nombre/etiqueta del jugador IA si se distingue del genérico "Jugador N".
- Respetar `AGENTS.md`: colores desde `MaterialTheme.colorScheme.*`, tipografía
  desde `MaterialTheme.typography.*`, `@Preview` para todo `@Composable` nuevo,
  importar los miembros de enum directamente (no calificados).

## 9) Estrategia de testing
### Unit tests (`core`, `commonTest`)
- Por cada nivel (`EASY`, `MEDIUM`, `HARD`) vía `chooseAiMove`:
  - Devuelve siempre una jugada **legal** (pertenece a `getLegalMoves`).
  - Determinismo con `Random` sembrado (misma semilla ⇒ misma jugada).
  - Devuelve `null` sólo si no hay jugadas legales / `GAME_OVER`.
  - Casos borde: peón bloqueado que requiere salto, cercanía a la meta,
    `wallsRemaining == 0` (no intenta colocar muros).
- Diferenciación de comportamiento:
  - `MEDIUM` prefiere avanzar por el camino más corto en posiciones abiertas y
    coloca muro cuando aumenta más `dRival` que `dSelf` (caso construido).
  - `HARD` elige, en un escenario construido, una jugada de mayor score
    (`dRival - dSelf`) que la que elegiría `MEDIUM`/`EASY`.
  - `EASY` es más aleatorio/menos óptimo (verificable con semilla controlada).
- `AiStrategy.forDifficulty` mapea cada `AiDifficulty` a la estrategia correcta.
- Helper de distancia (si se añade `shortestPathLength`): valores correctos con y
  sin muros, y `null`/valor especial si no hay camino.

### Unit tests (`app/shared`, `commonTest`)
- Selección de modo/dificultad:
  - Seleccionar `VERSUS_AI` navega al submenú de dificultad (no arranca partida).
  - Seleccionar un nivel arranca la partida con esa `AiDifficulty`.
  - `LOCAL_1V1` sigue arrancando la partida directamente.
  - `GameMode.VERSUS_AI.enabled == true`.
- `GameViewModel` en modo IA:
  - Tras jugada del humano, el estado avanza y el turno vuelve al humano (la IA
    jugó) — inyectar una estrategia/`Random` determinista o un `AiStrategy` fake.
  - `isAiThinking`/interacción bloqueada durante el turno de la IA.
  - Entrada del humano ignorada mientras la IA actúa.
  - Nueva partida cancela turnos de IA pendientes.
  - La dificultad recibida se usa para construir la estrategia.

> Nota: para tests deterministas del `ViewModel`, inyectar la `AiStrategy` (o un
> dispatcher/`Random` controlado) en vez de construir la real internamente.

## 10) Invariantes
1. La IA nunca aplica una jugada ilegal (todas pasan por validación de reglas).
2. La IA sólo juega en su turno y sólo si `IN_PROGRESS`.
3. La UI no permite entrada del humano mientras la IA decide/mueve.
4. La decisión de la IA es una función pura del `(state, playerId, random)` — sin
   estado oculto en dominio.
5. `domain/ai` no depende de UI ni de corrutinas.

## 11) Definición de terminado (DoD)
- `GameMode.VERSUS_AI` disponible; al elegirlo se abre el **submenú de dificultad**
  con los tres niveles y desde ahí arranca la partida.
- Los **tres niveles** (`EASY`, `MEDIUM`, `HARD`) implementados en
  `core/domain/ai`, diferenciados en comportamiento y expuestos por `QuoridorApi`.
- Partida vs IA jugable de principio a fin (2 jugadores) en cualquier nivel.
- Orquestación de turnos de IA en `GameViewModel` con bloqueo de interacción y uso
  de la dificultad seleccionada.
- Tests unitarios de IA (por nivel), del mapeo de dificultad, del submenú y de la
  orquestación pasando (`./gradlew :core:test` y el target de `app/shared`
  correspondiente).
- Sin colores/tipografías/textos hardcodeados; `@Preview` en composables nuevos.
- Código compila y no deja imports sin usar ni código muerto.
