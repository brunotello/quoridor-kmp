# Configuración de Firebase (modo online)

El modo **Online 1v1** usa **Firebase Realtime Database** a través del SDK KMP de
GitLive (`dev.gitlive:firebase-database`). El código ya está integrado; sólo
falta **crear el proyecto en Firebase y añadir las credenciales** por plataforma.

> Plataformas soportadas por GitLive: **Android, iOS, JVM (desktop) y JS**.
> `wasmJs` no está soportado y el modo online aparece deshabilitado ahí.

---

## 1) Crear el proyecto en Firebase

1. Entrá a la consola: **https://console.firebase.google.com/**
2. **Agregar proyecto** → nombre (p. ej. `quoridor`) → crear.
3. En el menú lateral: **Compilación → Realtime Database** → **Crear base de datos**.
   - Elegí la región.
   - Empezá en **modo de prueba** (reglas abiertas) para el prototipo. Para
     producción, restringí las reglas (ver §5).

---

## 2) Android

1. En la consola, **Configuración del proyecto → Tus apps → Agregar app → Android**.
2. **Nombre del paquete:** `com.btello.quoridor`
3. Descargá el archivo **`google-services.json`**.
4. Copialo en: **`app/androidApp/google-services.json`**
   (hay una plantilla en `app/androidApp/google-services.json.example`).

El plugin de Google Services se aplica **automáticamente** cuando ese archivo
existe (ver `app/androidApp/build.gradle.kts`), y Firebase se inicializa solo al
arrancar la app (no hace falta código extra). El archivo está en `.gitignore`.

---

## 3) iOS

1. En la consola, **Agregar app → iOS**.
2. **Bundle ID:** el de tu proyecto Xcode (p. ej. `com.btello.quoridor`).
3. Descargá **`GoogleService-Info.plist`** y agregalo al target de la app en Xcode.
4. En el arranque de la app iOS (`AppDelegate` / `@main`), llamá a
   `FirebaseApp.configure()` antes de mostrar la UI de Compose.

El archivo `GoogleService-Info.plist` también está en `.gitignore`.

---

## 4) Desktop (JVM) y Web (JS)

Estas plataformas **no** tienen inicialización automática (no leen
`google-services.json`). GitLive las soporta, pero requieren inicializar Firebase
manualmente con `FirebaseOptions` al arrancar, usando los valores del proyecto
(App ID, API key, `databaseUrl`, `projectId`). Es un paso opcional: el modo
online sigue funcionando en Android/iOS aunque no lo configures en desktop/web.

Los valores se obtienen en **Configuración del proyecto → General → Tus apps**
(o del propio `google-services.json`):
- `applicationId` → `mobilesdk_app_id`
- `apiKey` → `api_key.current_key`
- `databaseUrl` → `project_info.firebase_url`
- `projectId` → `project_info.project_id`

---

## 5) Reglas de Realtime Database

Para el **prototipo** (sin autenticación) alcanza con lectura/escritura abiertas:

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

⚠️ **No usar reglas abiertas en producción.** Para producción, agregá
autenticación (p. ej. anónima con `firebase-auth`) y restringí el acceso al nodo
`matches/{id}` a los participantes de la sala.

---

## 6) Verificación

- **Android:** con `google-services.json` en su lugar, compilá e instalá la app;
  el modo **Online** debe aparecer habilitado. Creá una sala en un dispositivo y
  unite con el código desde otro.
- **Sin credenciales:** el proyecto compila igual; en Android el modo online
  fallaría en tiempo de ejecución al conectar (por eso configurá antes de probar).
