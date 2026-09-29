package com.btello.quoridor.domain.online

import com.btello.quoridor.domain.model.GameConfig
import com.btello.quoridor.domain.model.GameState
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de sincronización de partidas online, sin conocer detalles de
 * infraestructura (Firebase u otro backend). Vive en el dominio para respetar la
 * regla de dependencias de Clean Architecture: la implementación concreta (capa
 * de datos) depende de esta interfaz, nunca al revés.
 *
 * El juego (reglas) sigue siendo autoritativo en `core`: quien publica un estado
 * ya lo validó con las reglas, y quien lo recibe puede volver a validarlo.
 */
interface OnlineGameRepository {

    /**
     * Crea una sala nueva en estado [MatchStatus.WAITING] con el [GameState]
     * inicial de [config] y devuelve su [MatchId] (código de sala compartible).
     * El anfitrión ocupa el asiento [PlayerSlot.HOST] con nombre [hostName] y la
     * sala queda marcada con [appVersion] (la versión de la app local) para que
     * sólo se unan dispositivos compatibles. Si [isPublic] es `true`, la sala se
     * anuncia en el lobby público ([observeOpenMatches]) para que otros se unan
     * sin conocer el código.
     */
    suspend fun createMatch(
        config: GameConfig,
        hostName: String,
        appVersion: String,
        isPublic: Boolean = false,
    ): MatchId

    /**
     * Flujo en tiempo real de las partidas públicas abiertas (en
     * [MatchStatus.WAITING], marcadas como públicas y con asientos libres),
     * limitado a las creadas con la misma [appVersion] local para no ofrecer
     * salas incompatibles.
     */
    fun observeOpenMatches(appVersion: String): Flow<List<OnlineMatch>>

    /**
     * Se une a la sala [id] con nombre [playerName], ocupando el siguiente asiento
     * libre (devuelto como [PlayerSlot]). Falla si la sala no existe, no está en
     * [MatchStatus.WAITING], ya está completa o fue creada con una versión de app
     * distinta a [appVersion] (en cuyo caso el error es
     * [IncompatibleVersionException]). Al ocuparse el último asiento la partida
     * pasa a [MatchStatus.IN_PROGRESS].
     */
    suspend fun joinMatch(id: MatchId, playerName: String, appVersion: String): Result<PlayerSlot>

    /** Flujo en tiempo real del estado de la sala [id]. */
    fun observeMatch(id: MatchId): Flow<OnlineMatch>

    /**
     * Publica un [newState] ya validado localmente. [expectedVersion] es la nueva
     * versión (monotónica); la implementación debe rechazar la escritura si otra
     * más reciente ya fue aplicada, para evitar sobrescribir jugadas ajenas.
     */
    suspend fun submitMove(id: MatchId, newState: GameState, expectedVersion: Long): Result<Unit>

    /** Marca al [slot] local como ausente (abandono / salida de la partida). */
    suspend fun leaveMatch(id: MatchId, slot: PlayerSlot)
}
