package com.btello.quoridor.domain.stats

/**
 * Puerto de dominio para persistir y recuperar los registros de partidas.
 *
 * El dominio define la abstracción; la capa de datos provee la implementación
 * concreta (almacenamiento por plataforma). Así se invierte la dependencia:
 * presentación y dominio no conocen los detalles de persistencia.
 */
interface StatisticsRepository {
    /** Registra una partida finalizada. */
    fun record(record: GameRecord)

    /** Devuelve todos los registros almacenados, del más antiguo al más reciente. */
    fun records(): List<GameRecord>

    /** Elimina todos los registros almacenados. */
    fun clear()
}
