import UIKit
import SwiftUI
import Shared

/// Hospeda un `ComposeUIViewController` (contenido de una pestaña) creado por Kotlin.
struct ComposeTabView: UIViewControllerRepresentable {
    let factory: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController { factory() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Fallback pre-iOS 26: app completa en Compose con la bottom bar Material.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Barra de pestañas nativa con estética Liquid Glass (iOS 26+).
/// El sistema aplica el efecto automáticamente por usar `TabView`; Compose sólo
/// renderiza el contenido de cada pestaña.
@available(iOS 26.0, *)
struct LiquidGlassTabView: View {
    var body: some View {
        TabView {
            Tab("Juego", systemImage: "play.circle") {
                ComposeTabView(factory: { TabViewControllersKt.GameTabViewController() })
                    .ignoresSafeArea(.all)
            }
            Tab("Reglas", systemImage: "list.bullet") {
                ComposeTabView(factory: { TabViewControllersKt.RulesTabViewController() })
                    .ignoresSafeArea(.all)
            }
            Tab("Ajustes", systemImage: "gearshape") {
                ComposeTabView(factory: { TabViewControllersKt.SettingsTabViewController() })
                    .ignoresSafeArea(.all)
            }
        }
        .tabBarMinimizeBehavior(.automatic)
        .tint(Color.accentColor)
    }
}

struct ContentView: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            LiquidGlassTabView()
        } else {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}
