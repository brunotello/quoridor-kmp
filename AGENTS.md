# AGENTS.md — Reglas de implementación (Quoridor)

Estas reglas son **obligatorias** y deben cumplirse SIEMPRE al generar o modificar código en este proyecto.

Stack: Kotlin Multiplatform + Compose Multiplatform + Material 3. Tests con `kotlin.test`.

## 1. Colores y tema
- **Prohibido hardcodear colores.** No usar `Color(0x...)`, `Color.Red`, valores hex ni literales de color en las pantallas/componentes.
- Todos los colores deben venir del tema Material Design 3: `MaterialTheme.colorScheme.*` (p. ej. `primary`, `onBackground`, `surfaceVariant`).
- Si se necesita un color nuevo, definirlo en el `ColorScheme`/tema (Material Design), no en el componente.

## 2. Tipografía y textos
- Todo `Text(...)` debe aplicar un estilo del tema: `style = MaterialTheme.typography.*` (p. ej. `bodyMedium`, `titleLarge`).
- **Prohibido hardcodear** valores tipográficos en los `Text` (nada de `fontSize`, `fontWeight`, `color`, `lineHeight` sueltos). Si un estilo no existe, agregarlo/ajustarlo en la `Typography` del tema.
- **Los textos visibles deben ir en `strings.xml`.** Prohibido dejar literales de UI en el código Kotlin. Definirlos en `*/src/commonMain/composeResources/values/strings.xml` (Compose Multiplatform Resources) y consumirlos con `stringResource(Res.string.<clave>)` (import `org.jetbrains.compose.resources.stringResource` y `...generated.resources.Res`).
  - Para textos con parámetros usar placeholders posicionales (`%1$d`, `%1$s`) y pasarlos como argumentos: `stringResource(Res.string.winner, numero)`.
  - Como `stringResource` es `@Composable`, las capas no-Compose (p. ej. `ViewModel`) no deben resolver textos: exponen un tipo/estado tipado y la resolución a string se hace en el `@Composable`.

## 3. Imports de constantes / enums
- Importar la entrada concreta, no el contenedor. En vez de usar `Screen.CONFIG`, importar el valor y usar `CONFIG`.
  - Correcto: `import com.btello.quoridor.Screen.CONFIG` → usar `CONFIG`.
  - Evitar: usar `Screen.CONFIG` calificado en el cuerpo del código.
- Aplica igual a otras constantes/enums: importar el miembro directamente en lugar de calificarlo con el contenedor.

## 4. Tests unitarios
- **Cada función nueva o modificada debe tener su test unitario.** Sin excepción.
- Usar `kotlin.test` (`@Test`, `assertEquals`, etc.), siguiendo la estructura existente en `*/src/commonTest/` (y los source sets por plataforma cuando corresponda).
- Ubicar los tests en el módulo/source set correcto (`core`, `app/shared`, `server`).
- Cubrir casos borde, no solo el camino feliz.

## 5. Convenciones generales
- Escribir Kotlin idiomático; respetar el estilo y la organización de paquetes existentes (`com.btello.quoridor`).
- Compose: componer funciones `@Composable` pequeñas y reutilizables; extraer valores mágicos a constantes o al tema.
- **Todo `@Composable` debe tener su `@Preview`.** Cada componente/pantalla necesita al menos una preview (usar `@Preview` de Compose Multiplatform, `org.jetbrains.compose.ui.tooling.preview.Preview`), envuelta en el tema de la app.
- No agregar dependencias nuevas sin avisar primero.
- No dejar código muerto, `println` de depuración ni imports sin usar.
- **Prohibido dejar imports sin usar.** Eliminar todo import que no se utilice.
- Mantener la lógica de juego en `core` desacoplada de la UI.

## 7. Arquitectura (Clean Architecture)
- Seguir **Clean Architecture** de forma obligatoria, con separación clara por capas:
  - **Domain** (entidades, casos de uso / reglas de negocio): sin dependencias de frameworks ni de UI.
  - **Data** (repositorios, fuentes de datos): implementa las interfaces definidas en domain.
  - **Presentation / UI** (Compose, ViewModels): depende de domain, nunca al revés.
- **Regla de dependencias:** las dependencias apuntan hacia adentro (UI → domain ← data). El dominio no conoce a la UI ni a los detalles de datos.
- La lógica de negocio va en casos de uso / dominio (`core`), no en `@Composable` ni en la capa de datos.
- Usar interfaces/abstracciones para invertir dependencias entre capas.

## 6. Verificación antes de terminar
- El código debe compilar y los tests deben pasar (`./gradlew test` o el target acotado que corresponda).
- Revisar que no se hayan introducido colores/tamaños/estilos hardcodeados ni referencias calificadas evitables.
