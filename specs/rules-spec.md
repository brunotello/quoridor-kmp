# Quoridor KMP Spec (v0 - motor de reglas + tests)

## 1) Objetivo
Definir e implementar una primera versión del juego **Quoridor** en Kotlin Multiplatform con foco en:
- Motor de reglas en `core`.
- Cobertura de tests unitarios + invariantes.
- Sin UI jugable en esta etapa.

## 2) Alcance (v0)
Incluye:
- Modelo de dominio del tablero, jugadores, peones, muros y turnos.
- Engine de validación/aplicación de jugadas.
- Estados y errores de dominio tipados.
- Serialización de estado para persistencia/intercambio.
- Tests unitarios y tests de invariantes.

No incluye:
- UI jugable.
- Red/multiplayer online.
- IA avanzada.
- Animaciones, sonido o pulido UX final.

## 3) Reglas implementadas
- Jugadores: **2 o 4** (`GameConfig` valida `playerCount == 2 || == 4`, en otro caso `InvalidPlayerCount`).
- Tablero: **9x9** (`DEFAULT_BOARD_SIZE = 9`, configurable vía `boardSize`).
- Muros por jugador:
  - 2 jugadores: **10** (`TWO_PLAYER_WALLS`).
  - 4 jugadores: **5** (`FOUR_PLAYER_WALLS`).
- Posiciones y metas iniciales (columna/fila central del borde):
  - 2 jugadores:
    - `PlayerId(0)`: `Cell(0,4)`, meta `BOTTOM`.
    - `PlayerId(1)`: `Cell(8,4)`, meta `TOP`.
  - 4 jugadores (suma los dos anteriores):
    - `PlayerId(2)`: `Cell(4,0)`, meta `RIGHT`.
    - `PlayerId(3)`: `Cell(4,8)`, meta `LEFT`.
- Turno: una acción por turno (mover peón **o** colocar muro). Tras aplicar la jugada el turno pasa al siguiente jugador en orden circular por índice.
- Movimiento de peón:
  - Ortogonal a celda adyacente (arriba/abajo/izquierda/derecha), no diagonal directo.
  - No puede salir del tablero (`OutOfBounds`), atravesar un muro ni caer sobre una celda ocupada (`CellBlocked`).
  - `from` debe coincidir con la posición actual del peón (`CellBlocked` en caso contrario).
- Muros:
  - Anclados en `Cell(row, col)` con `row`/`col` en `0..size-2` (fuera de rango → `OutOfBounds`).
  - Un muro horizontal cubre las columnas `col` y `col+1`; uno vertical cubre las filas `row` y `row+1`.
  - No pueden solaparse: mismo orientación y misma línea con anclas a distancia ≤ 1 (`WallOverlap`).
  - No pueden cruzarse: orientación opuesta anclada en la misma `Cell` (`WallCrossing`).
  - No pueden dejar sin camino a la meta a **ningún** jugador; se valida con BFS sobre el tablero candidato (`WallBlocksAllPaths`).
  - Son permanentes (no se mueven ni retiran).
  - Si el jugador no tiene muros restantes solo puede mover (`NoWallsRemaining`).
- Salto:
  - Si un rival ocupa la celda adyacente y detrás de él (en línea) no hay muro ni otro peón, se salta a la celda posterior.
  - Si detrás del rival hay muro o peón, se habilitan los saltos diagonales que no estén bloqueados por muro ni ocupados.
  - Si ambas diagonales son legales, ambas valen.
- Victoria:
  - Un jugador gana al alcanzar el borde opuesto según su `goalSide`; el estado pasa a `GAME_OVER` y se fija `winner`.
  - Con `GAME_OVER` no se aceptan más jugadas (validación devuelve error).

## 4) Stack técnico
- Kotlin Multiplatform.
- Kotlin puro en `core`.
- `kotlinx.serialization`.
- `kotlin.test`.

## 5) Arquitectura por capas en `core`
Paquete raíz: `com.btello.quoridor`.
- `domain/model` (`DomainTypes.kt`): entidades, value objects, `Move`, `GameConfig`, `ValidationResult`, `MoveResult` y constantes (`DEFAULT_BOARD_SIZE`, `TWO_PLAYER_WALLS`, `FOUR_PLAYER_WALLS`).
- `domain/rules`:
  - `DomainError.kt`: jerarquía de errores de dominio.
  - `RuleEngine.kt`: `object QuoridorRules` con la lógica de reglas (validación, aplicación, jugadas legales, BFS de caminos, salto).
- `QuoridorApi.kt` (paquete raíz): funciones de nivel superior que delegan en `QuoridorRules` y `typealias` de conveniencia.
- Serialización: vía anotaciones `@Serializable` de `kotlinx.serialization` sobre los modelos (no hay un paquete `serialization` separado).

> Nota: aún no existen paquetes `domain/usecase` ni `domain/state`; la transición de estado se resuelve dentro de `QuoridorRules`.

## 6) Modelo de dominio (implementado)
- `GameState` (`board`, `players`, `turn`, `status`, `winner`)
- `Board` (`size`, `walls`; con `contains` e `isValidWall`)
- `Cell` (`row`, `col`); `PawnPosition` es `typealias` de `Cell`
- `PlayerId` (value class sobre `Int`)
- `Player` (`id`, `position`, `wallsRemaining`, `goalSide`)
- `GoalSide` (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`)
- `Wall` (`row`, `col`, `orientation`) y `WallOrientation` (`HORIZONTAL`, `VERTICAL`)
- `Turn` (`playerId`)
- `Move` (sealed interface): `Move.PawnMove` (`from`, `to`) y `Move.PlaceWall` (`wall`); `typealias PawnMove`/`WallMove`
- `GameStatus` (`IN_PROGRESS`, `GAME_OVER`)
- `GameConfig` (`playerCount`, `boardSize`)
- `ValidationResult` (`isValid`, `error`)
- `MoveResult` (`state`, `isSuccessful`, `error`)

Errores de dominio (`DomainError`, subtipo de `IllegalArgumentException`):
- `OutOfBounds`
- `CellBlocked`
- `NotPlayersTurn`
- `WallOverlap`
- `WallCrossing`
- `WallBlocksAllPaths`
- `NoWallsRemaining`
- `InvalidPlayerCount`

## 7) API de dominio (implementada)
Funciones de nivel superior en `QuoridorApi.kt` (delegan en `QuoridorRules`):
- `startGame(config: GameConfig): GameState`
- `startGame(playerCount: Int): GameState`
- `validateMove(state, move): ValidationResult`
- `applyMove(state, move): MoveResult`
- `getLegalMoves(state): List<Move>`
- `isGameOver(state): Boolean`

## 8) Invariantes globales
1. Cada jugador tiene exactamente un peón en celda válida.
2. Ningún peón comparte celda.
3. El turno activo siempre referencia un jugador existente.
4. Muros restantes nunca negativos.
5. No hay muros fuera de límites ni con geometría inválida.
6. Siempre existe al menos un camino a meta por jugador.
7. Si `GameOver`, existe ganador válido y no se aceptan más jugadas.

## 9) Estrategia de testing
### Unit tests
- Por regla (happy path + errores).
- Por tipo de error de dominio.
- Turnos y finalización.

### Invariantes
- Secuencias de jugadas válidas preservan invariantes.
- Estrés de colocación de muros.
- Casos de regresión.

## 10) Definición de terminado (DoD)
- Motor implementado en `core`.
- Reglas implementadas y cubiertas con tests.
- Invariantes verificadas por tests.
- Sin errores silenciosos.
- Serialización estable de `GameState`.
