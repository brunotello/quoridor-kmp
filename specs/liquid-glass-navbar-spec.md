# Quoridor — Bottom Navigation Bar: Material (Android) + Liquid Glass (iOS)

## 1) Objetivo
La barra de navegación inferior (bottom navigation) debe verse **nativa en cada
plataforma**:

- **Android / Desktop / Web:** barra Material 3 (`NavigationBar` de Compose), tal
  como está implementada hoy.
- **iOS 26+:** barra de pestañas nativa con estética **Liquid Glass** (traslúcida,
  flotante y con efecto de profundidad), renderizada por el sistema a través de un
  `TabView` de SwiftUI.
- **iOS < 26:** *fallback* a la barra Material de Compose (misma UI que Android).

Se siguen los pasos de la guía oficial de JetBrains
[Liquid Glass in a Compose Multiplatform app](https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html),
adaptados a la estructura de este proyecto.

## 2) Motivación
Liquid Glass (iOS 26) solo se aplica automáticamente cuando el sistema es dueño de
la barra de pestañas mediante componentes nativos (`TabView` / `NavigationStack`).
Con la barra dibujada 100% en Compose no es posible obtener el efecto real: hay que
ceder la barra a SwiftUI y dejar que Compose siga renderizando **el contenido de
cada pestaña**.

## 3) Alcance
Pestañas actuales (definidas en `Tabs`): **Juego**, **Reglas**, **Ajustes**.

Incluye:
- Barra nativa Liquid Glass en iOS 26 con las 3 pestañas.
- Cada pestaña renderiza su contenido Compose existente (`MainScreen`,
  `RulesScreen`, `SettingsScreen`) dentro de un `ComposeUIViewController`.
- El flujo de partida (Menú → `GameScreen` → volver) vive dentro de la pestaña
  **Juego**.
- Estado de tema (claro/oscuro) compartido entre las pestañas para que el toggle de
  Ajustes afecte a toda la app.
- *Fallback* automático a Compose/Material en iOS anteriores a 26.

No incluye (trade-offs / futuro):
- Empujar `GameScreen` a un `NavigationStack` nativo para **ocultar** la barra
  durante la partida. En esta versión la barra flotante sigue visible sobre el
  tablero (aceptable dado que es translúcida y se minimiza al hacer scroll). Migrar
  a `NavigationStack` por pestaña queda como mejora futura.
- Localización de las etiquetas de pestaña en Swift vía `Localizable.strings`
  (hoy se pasan literales que replican `strings.xml`).

## 4) Arquitectura

### Antes
```
ContentView (SwiftUI)
└── ComposeView  → MainViewController() → App() (NavBar Material en Compose)
```

### Después (iOS 26+)
```
ContentView (SwiftUI)
└── TabView (Liquid Glass)
    ├── Tab: Juego    → ComposeUIViewController → GameTab (MainScreen ↔ GameScreen)
    ├── Tab: Reglas   → ComposeUIViewController → RulesScreen
    └── Tab: Ajustes  → ComposeUIViewController → SettingsScreen
```

### iOS < 26 (fallback)
```
ContentView (SwiftUI)
└── ComposeView → MainViewController() → App() (NavBar Material en Compose)
```

Cada pestaña es un `ComposeUIViewController` independiente (composición Compose
separada). El estado de tema se comparte mediante snapshot state global
(`IosAppState.darkTheme`), que **todas** las composiciones observan y recomponen al
cambiar.

## 5) Cambios en el código

### 5.1 Kotlin compartido (`app/shared/src/iosMain`)
- **`IosAppState.kt`** — estado de tema compartido entre pestañas:
  `object IosAppState { val darkTheme = mutableStateOf(true) }`.
- **`GameTab.kt`** — composable que aloja el flujo de la pestaña Juego
  (Menú `MainScreen` ↔ `GameScreen`), replicando el switching que hace `App()`
  pero acotado a la pestaña.
- **`TabViewControllers.kt`** — puntos de entrada llamados desde Swift, cada uno
  devuelve un `UIViewController`:
  - `GameTabViewController()` → `GameTab`.
  - `RulesTabViewController()` → `RulesScreen`.
  - `SettingsTabViewController()` → `SettingsScreen` (toggle ligado a
    `IosAppState.darkTheme`).
  Cada VC envuelve el contenido en `QuoridorTheme(darkTheme = IosAppState.darkTheme)`.
- **`MainViewController.kt`** — se conserva `MainViewController()` (sin callbacks)
  como *fallback* pre-iOS 26 (Compose + NavBar Material).

### 5.2 Nativo iOS (`app/iosApp`)
- **`ContentView.swift`**:
  - `ComposeTabView: UIViewControllerRepresentable` genérico que hospeda un VC de
    Compose a partir de una factory.
  - `LiquidGlassTabView` (`@available(iOS 26.0, *)`): `TabView` con un `Tab` por
    pestaña, `.tabBarMinimizeBehavior(.automatic)` (barra flotante que se minimiza)
    y `.tint(Color(.accent))`.
  - `ContentView`: `if #available(iOS 26.0, *)` → `LiquidGlassTabView`, si no →
    `ComposeView` (fallback Material).
- El *accent* del tab bar usa el asset `AccentColor` ya presente en
  `Assets.xcassets`.

## 6) Controles nativos por plataforma
Además de la barra, los controles interactivos usan apariencia nativa vía
`expect/actual`:

- **`PlatformSwitch`** (`presentation/components/PlatformSwitch.kt`): `expect`
  común con `@Preview`.
  - Android / Desktop / Web: `actual` que delega en `MaterialSwitch` (Material 3).
  - iOS: `actual` que hospeda un `UISwitch` nativo mediante `UIKitView`
    (interop UIKit), con un target ObjC (`SwitchTarget`) que reenvía
    `valueChanged` a Compose.
- `SettingsScreen` usa `PlatformSwitch` en lugar de `Switch`, por lo que el toggle
  de tema se ve Material en Android y como el switch del sistema en iOS.

Este patrón (`expect/actual` + interop UIKit) es el recomendado para cualquier
control que deba verse nativo en iOS.

## 7) Estado del tema (claro/oscuro)
- Fuente de verdad en iOS 26: `IosAppState.darkTheme` (snapshot state global).
- `SettingsScreen` escribe `IosAppState.darkTheme.value` y lee el valor actual.
- Al ser snapshot state global, las 3 composiciones (una por pestaña) recomponen y
  el tema se aplica en toda la app sin coordinación adicional con Swift.
- En el *fallback* Compose (iOS < 26 y resto de plataformas) el tema sigue
  gobernado por el estado local de `App()`.

## 8) Comportamiento esperado (Liquid Glass, iOS 26)
- La barra es traslúcida, flota sobre el contenido y aplica profundidad/refracción
  automáticamente (lo renderiza el sistema; no se escribe CSS/estilo propio).
- Se minimiza al hacer scroll (`tabBarMinimizeBehavior(.automatic)`).
- La pestaña seleccionada usa el color de acento de la app.
- El contenido de cada pestaña lo dibuja Compose (misma UI/reglas que el resto de
  plataformas).

## 9) Fallback y compatibilidad
- iOS < 26 no expone las APIs de Liquid Glass ni el nuevo `TabView`; se usa la
  barra Material de Compose (idéntica a Android) vía `MainViewController()`.
- Android, Desktop y Web no se ven afectados: siguen usando `NavBar` Material.

## 10) Testing
- **Kotlin (`iosTest`):** test de `IosAppState` (valor por defecto y toggle).
- **Compose:** `@Preview` para los composables nuevos (`GameTab`).
- **Swift/UI:** verificación manual en simulador iOS 26 (aspecto Liquid Glass,
  cambio de pestaña, toggle de tema) y en un simulador iOS < 26 (fallback Material).
  No hay tests automatizados de UI nativa.

## 11) Criterios de aceptación
1. En Android/Desktop/Web la bottom bar se ve Material (sin cambios).
2. En iOS 26 la barra inferior es un `TabView` nativo con estética Liquid Glass y
   3 pestañas (Juego, Reglas, Ajustes).
3. Cada pestaña muestra el contenido Compose correcto.
4. El toggle de tema en Ajustes cambia el tema de toda la app en iOS 26.
5. En iOS < 26 la app cae al *fallback* Material sin fallar la compilación.
6. `./gradlew test` (o el target acotado) pasa, sin colores/estilos hardcodeados en
   Compose.
