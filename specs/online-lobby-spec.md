# Quoridor Online Lobby Spec (partidas públicas + abandono)

## 1) Objetivo
Extender el modo online (ver `online-multiplayer-spec.md`) con un **lobby
público** y la posibilidad de **abandonar** una partida online desde el botón de
volver. En concreto:

- Poder **crear una partida pública**, que se anuncie automáticamente en el lobby.
- Poder **ver las partidas públicas abiertas** y **unirse sin ingresar ningún
  código**.
- Un **botón de refrescar** (o pull-to-refresh) en el listado del lobby.
- Al **volver atrás** durante una partida online (flecha o gesto/botón del
  sistema), advertir al usuario con un diálogo de confirmación antes de
  abandonar: en 1v1 pierde y gana el rival; en 4 jugadores se lo quita de la
  partida (aunque sea el creador) y el resto continúa jugando.

Se respeta Clean Architecture: el contrato vive en `core` (domain) y la
implementación con Firebase en la capa de datos (`app/shared/data`). No se agregan
dependencias nuevas.

## 2) Alcance
Incluye:
- Marcar una sala como **pública/privada** al crearla (toggle en el lobby). Toda
  sala mantiene su `matchId` compartible; sólo las públicas se listan.
- Listado en tiempo real de las salas públicas abiertas (en `WAITING`, con
  asientos libres) para unirse sin código.
- Refresco manual del listado.
- Abandonar una partida online desde el botón de volver: en 1v1 pierde el que se
  va; en 4 jugadores se lo quita y el resto continúa.

No incluye:
- Matchmaking automático / emparejamiento aleatorio (el usuario elige la sala de
  la lista manualmente).
- Filtros/orden avanzado del listado, paginación o búsqueda por texto.

## 3) Dominio (`core`, `com.btello.quoridor.domain.online` y `domain.rules`)
- `OnlineMatch` gana un campo `isPublic: Boolean` (por defecto `false`) y las
  propiedades derivadas:
  - `hostName`: nombre del asiento `HOST` (`playerNames.firstOrNull().orEmpty()`).
  - `isOpenToPublic`: `isPublic && status == WAITING && !isFull`.
- `OnlineGameRepository`:
  - `createMatch(config, hostName, isPublic: Boolean = false): MatchId`.
  - `observeOpenMatches(): Flow<List<OnlineMatch>>` — emite las salas
    `isOpenToPublic`, ordenadas por `id`.
- `QuoridorRules.withPlayerRemoved(state, playerId)`: quita al jugador del
  `GameState`; si era su turno, avanza al siguiente; si queda un único jugador,
  este gana (`GAME_OVER`). Unifica el caso 1v1 (queda 1 → gana el rival) y el de 4
  jugadores (quedan ≥2 → sigue en juego). El **abandono** no necesita contrato
  online nuevo: se publica el nuevo `GameState` con el `submitMove`/`version`
  existente (si termina, la sala pasa a `FINISHED`).

## 4) Datos (`app/shared`, `com.btello.quoridor.data.online`)
- `FirebaseOnlineGameRepository`:
  - `createMatch` persiste `isPublic`.
  - `observeOpenMatches` lee `matches` (`valueEvents`), mapea cada hijo a
    `OnlineMatch` y filtra por `isOpenToPublic`.
- `FakeOnlineGameRepository` (test): mismo comportamiento en memoria, con un
  contador de revisión para recomponer el listado ante cada mutación.

## 5) Presentación (`app/shared`, `presentation/online` y `presentation/game`)

### 5.1 Lobby (`OnlineLobbyScreen` / `OnlineLobbyViewModel`)
- Paso nuevo `Browse` (explorar salas públicas). El menú ofrece, **en este
  orden**: Crear partida, Unirse por código y, **como último elemento**, Buscar
  partidas.
- Estado (`OnlineLobbyUiState`): `isPublic` (para crear), `openMatches:
  List<OnlineOpenMatch>` (resumen `id`, `hostName`, `joinedCount`, `playerCount`).
- Eventos: `ChooseBrowse`, `VisibilityChanged(isPublic)`,
  `JoinPublicMatch(id)` y `RefreshBrowse`.
- El paso `Create` incluye un `Switch` de visibilidad (público por defecto).
- El paso `Browse` muestra un botón de **refrescar** (reinicia la observación) y
  la lista de salas, cada una con un botón "Unirse" que dispara
  `JoinPublicMatch(id)` sin pedir código. Estado vacío con texto explicativo.
- Unirse (por código o público) reutiliza `joinMatch`; al éxito detiene la
  observación y emite `StartGame`.

### 5.2 Juego (`GameScreen` / `GameViewModel`)
- El botón de **volver** (flecha superior y back del sistema, vía
  `AppBackHandler`) enruta por `onBackRequested`: en partidas **online** muestra
  un diálogo de confirmación de **abandono**; en local vuelve directo al menú.
- El diálogo advierte según la cantidad de jugadores: en 1v1 ("perdés la partida
  y gana tu rival") y en 4 jugadores ("saldrás de la partida y el resto seguirá
  jugando").
- `GameEvent.LeaveMatch` → `onLeaveMatch()`: cancela los trabajos en curso; si es
  online y la partida no terminó, quita al jugador local con
  `QuoridorRules.withPlayerRemoved` y publica el estado con `submitMove` para que
  el resto lo reciba (el rival gana en 1v1; los demás siguen en 4 jugadores).
  Luego navega al menú.
- La pantalla de resultado (`GameResultScreen`) ya muestra al ganador.

## 6) Textos y estilo (AGENTS.md)
- Todos los textos nuevos en `strings.xml` (y su traducción en `values-en`),
  consumidos con `stringResource`.
- Colores desde `MaterialTheme.colorScheme.*`, tipografías desde
  `MaterialTheme.typography.*`, `@Preview` en composables nuevos, imports de
  miembros directos, sin código muerto ni imports sin usar.
- Claves nuevas del lobby: `online_browse_match`, `online_browse_description`,
  `online_browse_empty`, `online_browse_refresh`, `online_public_label`,
  `online_public_description`, `online_open_match_host`,
  `online_open_match_players`.
- Claves nuevas del juego: `game_leave_title`, `game_leave_message_lose`,
  `game_leave_message_continue`, `game_leave_confirm`, `game_leave_dismiss`.

## 7) Testing (`commonTest`)
- Dominio (`core`): `isOpenToPublic`/`hostName`, round-trip de `isPublic`;
  `withPlayerRemoved` (1v1 gana el rival, 4p avanza turno/wrap, jugador fuera de
  turno intacto, jugador inexistente o partida terminada sin cambios).
- `OnlineLobbyViewModel` (con `FakeOnlineGameRepository`): crear pública/privada,
  listar salas abiertas, actualización al llenarse una sala, unirse sin código,
  refrescar el listado, volver del listado limpia el estado.
- `GameViewModel`: abandonar local no marca derrota; abandonar 1v1 online termina
  con el rival ganador (sala `FINISHED`); abandonar 4p online quita al jugador y
  el resto sigue (`IN_PROGRESS`).

## 8) Definición de terminado (DoD)
- Crear sala pública y verla en el listado sin código; unirse desde la lista.
- Botón de refresco funcionando.
- Volver atrás en online confirma el abandono y aplica la lógica 1v1 / 4
  jugadores; el resto de los jugadores puede continuar.
- Textos en `strings.xml` + traducción `values-en`; tema respetado; `@Preview` en
  composables nuevos; imports limpios.
- Tests unitarios en verde (`:core:test`, `:app:shared:test` o targets acotados).
