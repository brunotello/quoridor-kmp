# Quoridor UI Spec (Compose Multiplatform)

## 1) Objetivo
Definir una UI completa para Quoridor sobre **Compose Multiplatform**, desacoplada del motor en `core`, con foco en jugabilidad clara, feedback inmediato y UX pulida.

## 2) Alcance (implementado)
Incluye:
- Flujo completo de partida local para **2 jugadores** (Config → Game → Resultado).
- Render de tablero 9x9, peones (con animación de desplazamiento), muros colocados y ranuras de muro legales.
- Interacciones de movimiento y salto (a través de las jugadas legales del `core`) y colocación de muros.
- Banner de feedback para mensajes y errores, y pantalla de fin de partida.
- Coherencia visual cross-platform (Android, Desktop/JVM, iOS, Web/Wasm) con tema oscuro.

No incluye (aún):
- Flujo de UI para **4 jugadores** (el motor lo soporta, pero la config solo ofrece 2 y `GameScreen` solo dibuja los paneles de los jugadores 0 y 1).
- Toggle de modo de acción visible (`ActionModeButton` existe pero no se usa; el modo muro se activa tocando la reserva de muros del jugador activo).
- Diálogo modal de fin de partida (se usa una pantalla `GameResultScreen` a pantalla completa).
- Accesibilidad avanzada (no hay `contentDescription`, semantics ni soporte de teclado).
- Multiplayer online, matchmaking, chat/voz.

## 3) Principios UX
1. Claridad de turno.
2. Acciones guiadas.
3. Feedback inmediato.
4. Baja carga cognitiva.
5. Sin lógica duplicada de reglas en UI.

## 4) Arquitectura UI (implementada)
- Patrón: `ViewModel` (`QuoridorViewModel`, extiende `androidx.lifecycle.ViewModel`) con estado observable vía `mutableStateOf` (no hay reducer/intent formal de MVI).
- Fuente de verdad: `GameState` obtenido desde `core` (`QuoridorRules`).
- El `ViewModel` expone estado derivado para la UI: `screen`, `playerCount`, `gameState`, `feedback`, `showingWallTargets`, `legalWallTargets`.
- Navegación por un enum `Screen { CONFIG, GAME, RESULT }` conmutado en `App()`.
- Mensajes/errores se propagan por `feedback: String?` (no hay efectos one-shot dedicados).

Organización de archivos (paquete plano `com.btello.quoridor` en `app/shared/src/commonMain`):
- `App.kt`: root Compose + `MaterialTheme(darkColorScheme())` y ruteo por `Screen`.
- `QuoridorViewModel.kt`: estado e intents.
- `GameConfigScreen.kt`, `GameScreen.kt`, `GameResultScreen.kt`: pantallas.
- `BoardView.kt` + `BoardGrid.kt`: render del tablero y modelo de grilla.
- `ActionModeButton.kt`: componente auxiliar (actualmente sin uso).

> Nota: no existen paquetes `ui/screen`, `ui/components`, `ui/state` ni `ui/theme` separados; el tema se define inline en `App()`.

## 5) Pantallas y flujo (implementado)
### Config (`GameConfigScreen`)
- Título "Quoridor" y selector de jugadores (solo la opción **2 jugadores** está disponible).
- Resumen "Tablero 9x9 • N jugadores".
- Botón "Iniciar partida" → `viewModel.startGame()`.

### Game (`GameScreen`)
- Paneles de jugador arriba/abajo (`PlayerPanel`) con nombre "Jugador N", color y muros restantes; el panel activo permite activar el modo muro tocando la reserva.
- Banner de feedback condicional.
- Tablero 9x9 responsivo (`BoardView`) centrado, con `aspectRatio(1f)`.

### Resultado (`GameResultScreen`)
- "Partida finalizada" y "Ganador: Jugador N" (a partir de `state.winner`).
- Botón "Nueva partida" → `viewModel.restartGame()` (vuelve a Config).

## 6) Componentes clave (implementados)
- `BoardView`: dibuja la grilla, peones animados (`AnimatedPawn`), celdas (`CellBox`) y ranuras/intersecciones de muro (`WallBox`).
- `BoardGrid`: modelo que mapea el tablero NxN a una grilla (2N-1)x(2N-1) con `CellSlot`, `WallSlot` e `IntersectionSlot`, calculando celdas destino legales, muros cubiertos y ranuras legales.
- `PlayerPanel` (en `GameScreen`): indicador de turno + contador de muros restantes; sirve como disparador del modo muro.
- Banner de feedback: `Surface` inline en `GameScreen` (no un componente separado).
- `GameResultScreen`: pantalla de fin de partida (en lugar de un diálogo).
- `ActionModeButton`: definido pero sin uso.

## 7) Interacciones de reglas (implementadas)
Toda la legalidad se consulta a `QuoridorRules.getLegalMoves(state)`.
### Movimiento
- Se resaltan las celdas destino legales (fondo destacado + punto del color del jugador). Al tocar una celda destino se aplica la jugada.

### Salto
- Los saltos (lineales y diagonales) llegan incluidos en las jugadas legales del `core` y se muestran igual que un movimiento normal; no hay un resaltado visual diferenciado para el salto.

### Muros
- Tocar la reserva de muros del jugador activo carga `legalWallTargets` y resalta las ranuras legales.
- Tocar una ranura legal coloca el muro (sin paso de confirmación previo).
- Si no quedan muros o no hay ubicaciones legales, se informa por el banner de feedback.

## 8) Estados visuales (implementados)
- Config inicial.
- Modo muro activo (ranuras legales resaltadas, `showingWallTargets = true`).
- Destinos de movimiento resaltados.
- Feedback/error (banner con `feedback`).
- Cambio de turno (paneles activo/inactivo + animación del peón).
- Game over (pantalla de resultado).

## 9) Accesibilidad
Estado actual: **base mínima**. Actualmente la UI **no** define `contentDescription`, semantics ni soporte de teclado; el estado se comunica principalmente por color. Pendiente:
- Content descriptions en peones, celdas y muros.
- Soporte de teclado en desktop.
- No depender solo del color para comunicar estado.

## 10) Testing UI
Los tests actuales en `app/shared` (`SharedCommonTest` y equivalentes por plataforma) cubren lógica compartida; no hay tests de componentes/semantics de Compose todavía. Pendiente:
- Tests de componentes críticos.
- Tests de flujos principales.
- Tests de mensajes de error.
- Tests básicos de semantics/accesibilidad.

## 11) Integración con core
- La UI no valida reglas por sí misma: usa `QuoridorRules.startGame`, `getLegalMoves`, `applyMove` e `isGameOver`.
- Los errores se muestran usando `DomainError.message` directamente (no hay un mapper `DomainError -> UserMessage` dedicado).
