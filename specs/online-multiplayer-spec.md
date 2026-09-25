# Quoridor Online Multiplayer Spec (modo "Online" sin servidor propio)

## 1) Objetivo
Definir e implementar un **modo de juego online 1v1 entre dos dispositivos**
usando **Firebase Realtime Database** a través del SDK KMP de
[GitLive](https://github.com/GitLiveApp/firebase-kotlin-sdk) como único
"backend" (Backend-as-a-Service). No se escribe ni despliega ningún servidor
propio: Firebase actúa como punto de encuentro (lobby) y canal de
sincronización de estado entre los dos jugadores.

La lógica de juego sigue viviendo en `core` (dominio) y es la **fuente de
verdad de las reglas**. El online sólo transporta y sincroniza `GameState` /
`Move` ya validados. Se respeta Clean Architecture: el contrato de sincronización
se define como interfaz en `core` (domain) y la implementación con Firebase vive
en la capa de datos (`app/shared/data`), sin contaminar el dominio con Firebase.

## 2) Alcance (v0)
Incluye:
- Un modo `ONLINE` en la pantalla "Nueva partida" (`GameMode`), 2 jugadores.
- Un **lobby** para:
  - **Crear** una partida y obtener un **código de sala** (`matchId`) compartible.
  - **Unirse** a una partida existente ingresando ese código.
- Sincronización en tiempo real del `GameState` por turnos vía Realtime Database.
- Orquestación en `GameViewModel`: aplicar la jugada local (validada por
  `QuoridorRules`), publicarla, y aplicar las jugadas remotas al recibirlas.
- Bloqueo de la interacción cuando **no** es el turno del jugador local
  (reutilizando el patrón de `isAiThinking`).
- Manejo básico de presencia/desconexión (marcar partida abandonada si el rival
  se desconecta) usando `onDisconnect` de Realtime Database.
- Contrato de dominio (`OnlineGameRepository` + casos de uso) y su implementación
  con GitLive.
- Tests unitarios de la orquestación (con un `OnlineGameRepository` fake) y del
  mapeo/serialización de estado.

No incluye (aún):
- Matchmaking automático / emparejamiento aleatorio (sólo por código de sala).
- Ranking online, cuentas de usuario, chat, reconexión avanzada / re-sync tras
  perder conexión prolongada.
- Anti-cheat robusto (ver §10, modelo de confianza cliente-autoritativo).
- Online para **4 jugadores** (el contrato se diseña extensible, pero v0 es 1v1).

## 3) Targets soportados (⚠️ limitación de plataforma)
El SDK GitLive Firebase (`dev.gitlive:firebase-database:2.1.0`) soporta
**Android, iOS, JVM (desktop) y JS (browser)**. **No** soporta `wasmJs`, que es
uno de los targets actuales del proyecto (`app/shared`). Por lo tanto:

- El modo `ONLINE` está disponible en Android, iOS, desktop (JVM) y web-JS.
- La disponibilidad se resuelve con `expect/actual` sobre `OnlinePlatform`
  (`isSupported` + `repositoryOrNull()`):
  - Un source set intermedio `firebaseMain` (padre de `androidMain`, `iosMain`,
    `jvmMain` y `jsMain`) provee la implementación con Firebase e
    `isSupported = true`.
  - En `wasmJsMain` devuelve `isSupported = false` (implementación no-op / `null`).
- `GameMode.ONLINE.enabled` (o su visibilidad en la UI) refleja
  `OnlinePlatform.isSupported`, de modo que en wasm el modo no se ofrezca
  (o se muestre deshabilitado con un texto explicativo).

> Alternativa futura: si se quiere online en wasm, usar la REST API de
> Realtime Database vía Ktor client (fuera del alcance de v0).

## 4) Arquitectura por capas (Clean Architecture)

```
core (domain, puro, sin Firebase)
 ├─ domain/online/model      → OnlineMatch, MatchId, MatchStatus, PlayerSlot, OnlineMatchConfig
 ├─ domain/online            → OnlineGameRepository (interface)
 └─ domain/online/usecase    → CreateMatch, JoinMatch, ObserveMatch, SubmitMove, LeaveMatch

app/shared (data + presentation, conoce Firebase sólo en data)
 ├─ data/online             → FirebaseOnlineGameRepository (GitLive), mappers, expect/actual factory
 └─ presentation/online     → OnlineLobby (crear/unirse), y orquestación en GameViewModel
```

Regla de dependencias: `presentation → domain ← data`. El dominio **no** importa
GitLive ni tipos de Firebase; sólo define interfaces y modelos serializables.

### 4.1 Dominio (`core`, paquete `com.btello.quoridor.domain.online`)
Modelos (todos `@Serializable`, reutilizando los ya serializables `GameState`,
`Move`, `GameConfig`, `PlayerId`):

```kotlin
@JvmInline @Serializable
value class MatchId(val value: String)

@Serializable
enum class MatchStatus { WAITING, IN_PROGRESS, FINISHED, ABANDONED }

/** Rol del dispositivo local dentro de la partida. */
enum class PlayerSlot { HOST, GUEST }

@Serializable
data class OnlineMatch(
    val id: MatchId,
    val config: GameConfig,
    val status: MatchStatus,
    val hostPresent: Boolean,
    val guestPresent: Boolean,
    val state: GameState,
    val version: Long,          // nº de jugada / versión monotónica para ordenar y detectar stale writes
)
```

Interfaz del repositorio (contrato de sincronización, sin Firebase):

```kotlin
interface OnlineGameRepository {
    /** Crea una sala nueva en estado WAITING con el estado inicial y devuelve su id. */
    suspend fun createMatch(config: GameConfig): MatchId

    /** Se une a una sala WAITING como GUEST; pasa la partida a IN_PROGRESS. */
    suspend fun joinMatch(id: MatchId): Result<Unit>

    /** Flujo del estado de la partida en tiempo real. */
    fun observeMatch(id: MatchId): Flow<OnlineMatch>

    /**
     * Publica una jugada ya validada localmente. Debe escribir el nuevo
     * GameState + version sólo si es el turno del slot local (ver §9/§10).
     */
    suspend fun submitMove(id: MatchId, newState: GameState, expectedVersion: Long): Result<Unit>

    /** Marca presencia/abandono; registra onDisconnect. */
    suspend fun leaveMatch(id: MatchId)
}
```

Casos de uso (envuelven el repo; una responsabilidad cada uno; sin corrutinas de
UI): `CreateMatchUseCase`, `JoinMatchUseCase`, `ObserveMatchUseCase`,
`SubmitMoveUseCase`, `LeaveMatchUseCase`. Cada uno delega en
`OnlineGameRepository` y aplica reglas de dominio donde corresponda (p. ej.
`SubmitMoveUseCase` valida con `QuoridorRules.validateMove` antes de publicar).

> El dominio expone `Flow` (kotlinx.coroutines), no tipos de Firebase. GitLive ya
> ofrece `Flow` en su API (`DatabaseReference.valueEvents`), que la capa de datos
> mapea a `OnlineMatch`.

### 4.2 Datos (`app/shared`, paquete `com.btello.quoridor.data.online`)
- `FirebaseOnlineGameRepository : OnlineGameRepository` implementado con GitLive:
  - `Firebase.database.reference("matches").child(matchId)`.
  - Lectura reactiva: `ref.valueEvents.map { it.value<OnlineMatchDto>() }`.
  - Escritura: `ref.setValue(dto)` / actualizaciones puntuales con
    `child("state").setValue(...)` y `child("version").setValue(...)`.
  - Presencia: `ref.child("<slot>Present").onDisconnect().setValue(false)` y
    marcar `status = ABANDONED` vía `onDisconnect` cuando aplique.
  - Serialización con kotlinx.serialization (GitLive integra
    `@Serializable`). Se puede usar un `OnlineMatchDto` de datos (mapeado a/desde
    `OnlineMatch`) o serializar `GameState` a JSON string bajo `state`.
- `expect/actual` factory:
  - `expect fun createOnlineGameRepository(): OnlineGameRepository?`
    (o un `OnlineAvailability` con `isSupported`).
  - `firebaseMain` (android/ios/jvm/js): devuelve `FirebaseOnlineGameRepository`.
  - `wasmJsMain`: devuelve `null` / no-op y `isSupported = false`.
- Mappers `OnlineMatchDto ↔ OnlineMatch` en data (el DTO puede aplanar la
  estructura para Realtime DB).

### 4.3 Presentación (`app/shared`, paquete `com.btello.quoridor.presentation`)
- **Lobby online** (`presentation/online`):
  - `OnlineLobbyScreen` (`@Composable` + `@Preview`) con dos acciones: "Crear
    partida" (muestra el `matchId` para compartir y espera al rival) y "Unirse"
    (campo para pegar el código). Estados: creando, esperando rival, uniéndose,
    error (código inexistente / sala llena / sin conexión).
  - `OnlineLobbyViewModel` que usa `CreateMatchUseCase` / `JoinMatchUseCase` /
    `ObserveMatchUseCase`. Al pasar la sala a `IN_PROGRESS`, emite un side effect
    que navega a `GameScreen` con un `GameSetup` de modo online.
- `GameMode.ONLINE`: nueva entrada del enum (título, descripción, emoji), 2
  jugadores, `aiCount = 0`. Al elegirlo se navega al lobby en vez de arrancar la
  partida directamente (patrón análogo a `requiresDifficulty` → un
  `requiresLobby`/manejo específico en `MainViewModel`).
- `GameSetup`: añadir la info de online sin contaminar el dominio. Preferido:
  un campo opcional `online: OnlineSession?` con `matchId: MatchId` y
  `localSlot: PlayerSlot` (qué `PlayerId` controla este dispositivo). Cuando
  `online != null`, `GameViewModel` corre en modo online.
- `GameViewModel` (modo online):
  - Recibe el `OnlineGameRepository` (o los casos de uso) y el `OnlineSession`.
  - `localPlayerId`: `HOST → PlayerId(0)`, `GUEST → PlayerId(1)`.
  - `observeMatch(matchId)` en `viewModelScope`: cada `OnlineMatch` recibido cuya
    `version` sea mayor que la local **reemplaza** el `gameState` local (aplica la
    jugada del rival) y refresca la UI.
  - Al aplicar una jugada **local** válida: incrementa `version`, publica con
    `submitMoveUseCase(matchId, newState, expectedVersion)`. Si la escritura falla
    por conflicto de versión, re-sincroniza desde el flujo remoto.
  - `isHumanInputBlocked()` es `true` cuando `gameState.turn.playerId != localPlayerId`
    (no es mi turno) — reutiliza el mecanismo de bloqueo existente
    (`isAiThinking` / interacción deshabilitada).
  - `NewGame` / salir: llama `leaveMatch(matchId)` y cancela la observación.
  - No hay IA en modo online (`aiPlayers` vacío); las ramas de IA quedan inertes.
- `GameScreen`/`BoardView`: sin cambios de comportamiento salvo respetar el flag
  de interacción ya existente. Se puede mostrar un indicador "Esperando al
  rival…" (texto en `strings.xml`) cuando no es tu turno o el rival no llegó.

## 5) Modelo de datos en Realtime Database
Estructura bajo la ruta `matches/{matchId}` (un único nodo por partida):

```
matches/
  {matchId}/
    config:      { playerCount: 2, boardSize: 9 }
    status:      "WAITING" | "IN_PROGRESS" | "FINISHED" | "ABANDONED"
    hostPresent: true|false
    guestPresent:true|false
    version:     <Long>            // nº de jugada aplicada; monotónico
    state:       <GameState serializado>   // fuente de verdad del tablero
    updatedAt:   <timestamp servidor>
```

- El `matchId` es corto y legible para compartir (p. ej. 6 caracteres
  alfanuméricos en mayúsculas, generados en `createMatch`; reintentar si colisiona).
- `state` guarda el `GameState` completo (Quoridor es por turnos y el estado es
  pequeño): sincronización simple e idempotente. `version` permite ordenar y
  descartar escrituras obsoletas.
- Presencia: al conectarse, cada slot pone su `*Present = true` y registra
  `onDisconnect().setValue(false)`. Si un slot se cae, el otro ve
  `*Present = false` y puede marcar `status = ABANDONED`.

## 6) Configuración de Firebase (setup, sin código de servidor)
- Crear un proyecto en la consola de Firebase y habilitar **Realtime Database**.
- Registrar apps: Android (`google-services.json`), iOS
  (`GoogleService-Info.plist`) y Web (config JS). Ver docs de GitLive para la
  inicialización por plataforma.
- **Reglas de seguridad** de la Realtime Database (mínimas para v0; sin auth):
  restringir escritura a la forma esperada, exigir que `version` sea creciente y
  que sólo se escriba `state` cuando corresponde. Ejemplo de endurecimiento
  progresivo:
  - v0 (prototipo): lectura/escritura abierta a `matches/{matchId}` — **sólo para
    desarrollo**.
  - v1: habilitar **Firebase Authentication anónima** y reglas que validen que
    quien escribe es host o guest de esa sala y que `newData.version ===
    data.version + 1`.
- ⚠️ No commitear secretos: los archivos de config de Firebase para clientes no
  son secretos de servidor, pero seguir la política del repo. Documentar en
  `README`/setup cómo obtenerlos; no incluir credenciales privadas.

## 7) Dependencias nuevas (requiere aprobación previa — AGENTS.md)
Agregar al version catalog (`gradle/libs.versions.toml`) y a `app/shared`
(sólo en los source sets soportados):

- GitLive Realtime Database: `dev.gitlive:firebase-database` (KMP: android/ios/jvm/js).
- (Opcional v1) `dev.gitlive:firebase-auth` para auth anónima.
- Android: plugin `com.google.gms.google-services` + `google-services.json`.
- iOS: pods de Firebase vía el mecanismo que use GitLive (SPM/CocoaPods).

> Estas dependencias se aplican en el source set intermedio `firebaseMain`
> (android/ios/jvm/js). `wasmJsMain` no las incluye (ver §3). No agregar hasta
> confirmar con el
> mantenedor (regla "no agregar dependencias sin avisar").

## 8) Textos (strings.xml) y estilo
- Todos los textos visibles en
  `app/shared/src/commonMain/composeResources/values/strings.xml`, consumidos con
  `stringResource(Res.string.<clave>)`. Sin literales en Kotlin. Nuevas claves
  sugeridas:
  - `game_mode_online`, `game_mode_online_description`, `game_mode_online_emoji`.
  - `online_lobby_title`, `online_create_match`, `online_join_match`,
    `online_match_code`, `online_share_code`, `online_enter_code`,
    `online_waiting_opponent`, `online_opponent_turn`, `online_your_turn`.
  - Errores: `online_error_not_found`, `online_error_full`,
    `online_error_no_connection`, `online_unsupported_platform`.
  - Para el código en textos parametrizados usar placeholders posicionales
    (`%1$s`) y resolver el string en el `@Composable` (no en el ViewModel).
- Respetar AGENTS.md: colores desde `MaterialTheme.colorScheme.*`, tipografía
  desde `MaterialTheme.typography.*`, `@Preview` en todo `@Composable` nuevo
  (envuelto en `QuoridorTheme`), imports de miembros de enum directos (no
  calificados), sin código muerto ni imports sin usar.

## 9) Flujo de una partida online
1. Jugador A elige `ONLINE` → lobby → "Crear partida". Se llama
   `createMatch(GameConfig(2))`; se obtiene `matchId`; la UI lo muestra para
   compartir. A queda como `HOST` (`PlayerId(0)`), `status = WAITING`.
2. Jugador B elige `ONLINE` → "Unirse" → pega el código. `joinMatch(matchId)`
   valida que exista y esté `WAITING`; pasa a `IN_PROGRESS` y marca `guestPresent`.
   B es `GUEST` (`PlayerId(1)`).
3. Ambos observan `observeMatch(matchId)` y navegan a `GameScreen` con el
   `GameSetup` online. `gameState` inicial = `QuoridorRules.startGame(config)`.
4. Turno de A (`turn.playerId == PlayerId(0)`): A interactúa; B tiene la entrada
   bloqueada. A aplica su jugada localmente (validada), incrementa `version`, y
   `submitMove` escribe `state` + `version`.
5. B recibe el nuevo `OnlineMatch` por el flujo, reemplaza su `gameState`, y ahora
   es su turno (bloqueo invertido). Se repite hasta `GAME_OVER`.
6. Al terminar (`GameStatus.GAME_OVER`): se marca `status = FINISHED`; ambos ven
   el resultado. Se registran estadísticas del jugador local si aplica.
7. Salir/`NewGame`: `leaveMatch` pone presencia en false; si un jugador abandona
   antes de terminar, el otro ve `*Present = false` → `status = ABANDONED` y se le
   informa.

## 10) Modelo de confianza e invariantes
- **Reglas autoritativas en cliente**: sin servidor, cada cliente valida sus
  jugadas con `QuoridorRules` antes de publicar, y **también valida** las jugadas
  remotas recibidas (que la transición desde su estado previo sea legal). Si una
  transición remota es ilegal o inconsistente, se descarta / se marca error (no se
  aplica un estado corrupto).
- Invariantes:
  1. Un cliente sólo publica estado cuando es su turno
     (`gameState.turn.playerId == localPlayerId`).
  2. `version` es estrictamente creciente; escrituras con `expectedVersion`
     desactualizado se rechazan/re-sincronizan (evita "last write wins" ciego).
  3. El dominio (`core`) no depende de Firebase ni de UI; sólo interfaces + modelos
     serializables.
  4. La UI bloquea la entrada del jugador cuando no es su turno o falta el rival.
  5. En plataformas no soportadas (wasm) el modo online no se ofrece.
- v1 (opcional, con Firebase Auth + reglas): mover parte de estas garantías a las
  reglas de seguridad de Realtime Database para mitigar clientes maliciosos.

## 11) Estrategia de testing
### Unit tests de dominio/orquestación (`app/shared`, `commonTest`)
Usar un **`FakeOnlineGameRepository`** en memoria (con `MutableStateFlow<OnlineMatch>`)
para tests deterministas, sin Firebase:
- `GameViewModel` en modo online:
  - Aplicar jugada local válida actualiza `gameState`, incrementa `version` y
    llama `submitMove` con el `expectedVersion` correcto.
  - Recibir un `OnlineMatch` remoto con `version` mayor reemplaza el estado local
    y actualiza el turno.
  - Entrada del humano bloqueada cuando `turn.playerId != localPlayerId`.
  - `NewGame`/salir llama `leaveMatch` y cancela la observación.
  - Rival abandona (`*Present = false`) → estado abandonado reflejado en la UI.
- `OnlineLobbyViewModel`:
  - "Crear" → `createMatch` devuelve `matchId` y expone estado "esperando".
  - "Unirse" con código válido → `joinMatch` OK → navega al juego.
  - "Unirse" con código inexistente / sala llena → error tipado correcto.
- Casos de uso (`CreateMatch`, `JoinMatch`, `SubmitMove`, `ObserveMatch`,
  `LeaveMatch`) delegan en el repo y aplican validación donde corresponde
  (`SubmitMove` rechaza jugadas ilegales vía `QuoridorRules`).

### Serialización (`core` o `app/shared`, `commonTest`)
- Round-trip de `GameState`/`Move`/`OnlineMatch(Dto)`: serializar y deserializar
  produce un objeto equivalente (incluye muros, saltos, `winner`, etc.).

### Fuera de alcance de tests automáticos
- La integración real contra Firebase (network) se valida manualmente / con un
  entorno de pruebas; no se testea en `commonTest`.

## 12) Definición de terminado (DoD)
- Contrato de dominio `OnlineGameRepository` + modelos + casos de uso en
  `core/domain/online`, sin dependencias de Firebase/UI.
- `FirebaseOnlineGameRepository` (GitLive) en `app/shared/data/online` con
  `expect/actual` de disponibilidad por target.
- `GameMode.ONLINE` visible sólo en plataformas soportadas; lobby para crear/unir
  por código funcionando.
- Partida 1v1 online jugable de principio a fin entre dos dispositivos, con
  bloqueo de turno y sincronización por `version`.
- Manejo de abandono/desconexión básico (`onDisconnect` / presencia).
- Textos en `strings.xml`; colores/tipografías desde el tema; `@Preview` en
  composables nuevos; imports limpios; sin código muerto.
- Tests unitarios (con `FakeOnlineGameRepository`) y de serialización pasando
  (`./gradlew :app:shared:test` / target acotado y `:core:test`).
- Dependencias nuevas (GitLive/Firebase) aprobadas antes de integrarse.
```
