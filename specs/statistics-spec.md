# Quoridor Statistics Spec (pantalla "Estadísticas")

## 1) Objetivo
Registrar el resultado de cada partida finalizada y ofrecer una nueva pantalla
de **Estadísticas** (con su propia pestaña en el bottom navigation) que resuma el
desempeño del jugador. Las estadísticas persisten entre reinicios de la app.

## 2) Alcance
Incluye:
- Registro automático de cada partida al terminar (victoria/derrota, dificultad,
  duración, jugadas y muros del ganador).
- Persistencia multiplataforma con
  [`multiplatform-settings`](https://github.com/russhwolf/multiplatform-settings)
  (variante `no-arg`), serializando los registros como JSON.
- Nueva pestaña **Estadísticas** entre `Reglas` y `Ajustes`.
- Pantalla que muestra:
  - Cantidad total de partidas.
  - Desglose de partidas por **dificultad** con su **resultado** (ganadas/perdidas).
  - **Menor tiempo** de una partida ganada.
  - **Menor cantidad de movimientos** de una partida ganada.
  - **Menor cantidad de muros usados** en una partida ganada.

No incluye (aún): estadísticas por jugador en partidas locales multi-usuario,
histórico detallado partida por partida, ni sincronización en la nube.

## 3) Arquitectura (Clean Architecture)
- **Domain (`core`, `com.btello.quoridor.domain.stats`)**:
  - `GameRecord`: entidad `@Serializable` de una partida finalizada, vista desde
    el jugador humano (`PlayerId(0)`): `won`, `difficulty` (`AiDifficulty?`, `null`
    para 1 vs 1), `durationMillis`, `moveCount`, `wallsUsed`.
  - `GameStatistics` + `DifficultyBreakdown`: agregación pura vía
    `GameStatistics.from(records)`. Las "mejores marcas" se calculan sólo sobre
    partidas ganadas. El desglose se ordena por dificultad con las locales al final.
  - `StatisticsRepository`: puerto (interfaz) con `record`, `records`, `clear`.
- **Data (`app/shared`, `com.btello.quoridor.data.stats`)**:
  - `SettingsStatisticsRepository`: implementa el puerto con `Settings` +
    `kotlinx.serialization`. JSON corrupto ⇒ lista vacía (no falla).
  - `StatisticsProvider`: localizador perezoso de la instancia de producción.
- **Presentation (`app/shared`, `com.btello.quoridor.presentation.stats`)**:
  - `StatisticsUiState`, `StatisticsViewModel` (carga y recalcula con `refresh`),
    `StatisticsScreen` (+ `@Preview`).
  - `GameViewModel` cuenta jugadas/muros por jugador y, al terminar, registra el
    `GameRecord` (una única vez) a través del `StatisticsRepository` inyectado.
  - `Tabs`/`NavBar`: nueva entrada `STATS`.

## 4) Reglas de registro
- Se registra al detectarse `GAME_OVER`, exactamente una vez por partida.
- `won = winner == PlayerId(0)`; `moveCount`/`wallsUsed` son los del ganador.
- La duración se mide con `TimeSource.Monotonic` (inyectable para tests).
- En 1 vs 1 la dificultad es `null` (grupo "Local 1 vs 1" en el desglose).

## 5) UI / textos
- Todos los textos en `strings.xml` (`es` + `values-en`), consumidos con
  `stringResource`. Colores desde `MaterialTheme.colorScheme.*` y tipografía desde
  `MaterialTheme.typography.*`. `@Preview` para los composables nuevos.

## 6) Testing
- `core`: `GameStatisticsTest` (agregación, mejores marcas sólo de ganadas,
  desglose y orden).
- `app/shared`: `SettingsStatisticsRepositoryTest` (persistencia y datos
  corruptos), `StatisticsViewModelTest` (carga/refresh), `GameViewModelStatisticsTest`
  (registro único al ganar, no registra en curso).

## 7) Definición de terminado (DoD)
- Pestaña Estadísticas disponible y funcional en todas las plataformas.
- Registro persistente de partidas y agregados correctos.
- Sin colores/tipografías/textos hardcodeados; sin imports sin usar.
- `./gradlew :core:jvmTest` y los tests de `app/shared` pasando; compila en
  Android, JVM, JS, WasmJS e iOS.
