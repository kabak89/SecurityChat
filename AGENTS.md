# AGENTS.md

Guidance for AI agents working in the SecurityChat repository.

## About the project

SecurityChat is a secure messenger built with **Kotlin Multiplatform** and **Compose Multiplatform
**.
Targets: Android (`androidApp`) and iOS (`iosApp`), with shared code in `shared` and the
`common/*` /
`features/*` modules. Navigation and component lifecycle use **Decompose**; dependency injection
uses **Koin**.

## Repository structure

- `androidApp/`, `iosApp/` — platform entry points.
- `shared/` — app assembly: wires features and navigation together.
- `common/*` — reusable modules (`core-ui`, `core-domain`, `core-component`, `core-db`,
  `core-network`, `ui-kit`, `icons-kit`, `localization`, `settings`, `log`, etc.).
- `features/*` — product features, each split into layered modules (see below).
- `build-logic/` — Gradle convention plugin `securitychat.convention.base`.

Module list is defined in [settings.gradle.kts](settings.gradle.kts).

## Feature architecture (six modules)

Each feature is split into layered modules with the Gradle path
`:features:<name>:<name>-<layer>`:

| Module                 | Responsibility                                               |
|------------------------|--------------------------------------------------------------|
| `{name}-domain`        | Repository interfaces, domain entities, `ScopedModel`        |
| `{name}-data`          | `RepoImpl`, data mappers                                     |
| `{name}-data-storage`  | Storage, `*SM` entities, storage mappers                     |
| `{name}-ui`            | Compose screens, `State`/`Event`/`ViewModel` (`viewModelOf`) |
| `{name}-component-api` | Decompose root contract + `Child` (sealed)                   |
| `{name}-component`     | Implementation: `childStack`, Koin load/unload               |

Reference feature: `features/settings`. For the full skeleton, naming rules, dependency graph and
checklist, see `.cursor/skills/feature-template/SKILL.md` (and `reference.md` next to it).

## Dependency injection (Koin)

Where a Koin module is registered depends on how many places consume it:

- **Used by a single feature:** load it inside that feature's root component. Call
  `getKoin().loadModules(featureModules)` in `init` and `getKoin().unloadModules(featureModules)` in
  `doOnDestroy`, so the module lives only while the component is alive and is released on destroy.
  See
  [SettingsComponentImpl.kt](features/settings/settings-component/src/commonMain/kotlin/com/security/chat/multiplatform/features/settings/component/SettingsComponentImpl.kt).
- **Used in several features / app-wide:** move it into the `commonAppDiModules` list in
  [CommonAppDiModules.kt](shared/src/commonMain/kotlin/com/security/chat/multiplatform/di/CommonAppDiModules.kt).
  These modules are loaded once at Koin startup and stay for the whole app lifetime.

Rule of thumb: keep a module component-scoped for as long as it has a single consumer; promote it to
`commonAppDiModules` only once a second place needs it.

## Conventions

- **Namespace / packages:** `com.security.chat.multiplatform.<area>.<feature>.<layer>`.
- **Gradle paths and directories:** kebab-case (e.g. `features/settings/settings-ui`).
- **Type-safe accessors:** camelCase segments (e.g. `projects.features.settings.settingsDomain`).
- Every module applies `id("securitychat.convention.base")` and sets
  `conventionBasePlugin { namespace = "..." }`.
- Decompose child-component contracts live in the `...component.api` package.
- Apply `kotlinxSerialization` only where navigation config / payload serialization is needed.
- Do not cross layer boundaries (e.g. `ui` must not depend on `data`; `domain` depends on neither).
- **UI state stability:** in `{name}-ui` modules the screen `State` and every class it references
  (nested entities, list element types) must be annotated with `@Immutable`
  (`androidx.compose.runtime.Immutable`) so Compose can skip recompositions. Such entities live in
  the screen's `entity` package. Use `@Stable` instead only when the class exposes observable
  mutable state.
- **DB access:** in SQLDelight-backed `{name}-data-storage` modules use a single
  `DatabaseCreator<T>`
  instance per database (never create more than one for the same DB). Obtain the DB via
  `dbCreator.getDb()` and wrap every read/write query execution in
  `withContext(dispatcherProvider.IO)`; expose reactive queries through `dbCreator.dbFlow` with
  `.flowOn(dispatcherProvider.IO)`.
- **String mapping fallbacks:** when mapping a type to/from its string representation (enum or
  sealed-type discriminators, `*SM` storage mappers, etc.), any unknown or unrecognized value must
  be logged via `Log.e` before returning `null`. Never swallow it silently — an unmapped value means
  data is dropped, and the log is the only trace of it. See `JoinedMessageRow.toSM` in
  [ChatDataStorageMapper.kt](features/chat/chat-data-storage/src/commonMain/kotlin/com/security/chat/multiplatform/features/chat/data/storage/mapper/ChatDataStorageMapper.kt),
  which logs the unknown `type` before returning `null`.

## Code style

Code style is enforced by ktlint; all rules come from [.editorconfig](.editorconfig). Do not restate
or override them here.

## Comments

Add explanatory comments only when necessary. When added, they must use the `/** */` block form and
be
written in English only.

## Build commands

Build behavior is controlled by Gradle properties (defaults from
[build-logic/src/main/kotlin/com/securitychat/gradle/ConventionBasePluginExtension.kt](build-logic/src/main/kotlin/com/securitychat/gradle/ConventionBasePluginExtension.kt)):

- `-PserverEnv=dev|stage|prod` (default `dev`)
- `-PisDebug=true|false` (default `true`)
- `-PenableLogs=true|false` (default `true`)

```bash
# Release bundle, prod environment, no logs, no debug
./gradlew :androidApp:bundleRelease -PserverEnv=prod -PenableLogs=false -PisDebug=false

# Release APK, prod
./gradlew :androidApp:assembleRelease -PserverEnv=prod -PenableLogs=false -PisDebug=false

# Debug build for development, dev environment, with logs
./gradlew :androidApp:assembleDebug -PserverEnv=dev -PenableLogs=true -PisDebug=true
```

iOS targets are built from `iosApp` via Xcode. For fast Kotlin-side feedback without Xcode (catches
interop/compile errors), use:

```bash
# Compile the shared framework for the iOS simulator target
./gradlew :shared:compileKotlinIosSimulatorArm64
```

## iOS specifics

- **Entry point flow:** `iosApp/iosApp/iOSApp.swift` (`@main`, SwiftUI `WindowGroup`) →
  `ContentView.swift` (a `UIViewControllerRepresentable`) → `shared` `RootViewController.kt`
  `rootViewController()`, which returns the `ComposeUIViewController` hosting `RootContent`.
- **Edge-to-edge / safe area:** SwiftUI lays a `UIViewControllerRepresentable` *inside* the safe
  area by default, which crops the Compose content under the status bar / home indicator.
  `ContentView`
  must use `.ignoresSafeArea()` so Compose draws full-screen; insets are then handled inside Compose
  via `WindowInsets` (e.g. `Modifier.systemBarsPadding()`). New top-level screens must apply that
  padding themselves.
- **System bars:** per-platform appearance lives in `common/ui-kit` `theme/SystemBarsEffect.*.kt`
  (`expect`/`actual`, driven by the app theme from `AppTheme`). On Android it uses
  `enableEdgeToEdge`;
  on iOS the status bar style is set globally via `UIApplication.setStatusBarStyle`, which requires
  `UIViewControllerBasedStatusBarAppearance = false` in `iosApp/iosApp/Info.plist`.
- **Kotlin/Native UIKit gotchas:** many `UIViewController` members (e.g. `preferredStatusBarStyle`)
  are `final` in the bindings and cannot be overridden by subclassing — prefer the global
  `UIApplication`/`UIWindow` APIs. Read-only Obj-C properties map to Kotlin `val`s; CGRect/struct
  interop needs `@OptIn(ExperimentalForeignApi::class)`.

## Do not

- Break layer boundaries between feature modules.
- Wire a new feature's navigation into existing modules as part of scaffolding it (separate task).
- Add dependencies outside the version catalog.
