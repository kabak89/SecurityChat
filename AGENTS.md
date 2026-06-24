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

## Conventions

- **Namespace / packages:** `com.security.chat.multiplatform.<area>.<feature>.<layer>`.
- **Gradle paths and directories:** kebab-case (e.g. `features/settings/settings-ui`).
- **Type-safe accessors:** camelCase segments (e.g. `projects.features.settings.settingsDomain`).
- Every module applies `id("securitychat.convention.base")` and sets
  `conventionBasePlugin { namespace = "..." }`.
- Decompose child-component contracts live in the `...component.api` package.
- Apply `kotlinxSerialization` only where navigation config / payload serialization is needed.
- Do not cross layer boundaries (e.g. `ui` must not depend on `data`; `domain` depends on neither).

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

iOS targets are built from `iosApp` via Xcode.

## Do not

- Break layer boundaries between feature modules.
- Wire a new feature's navigation into existing modules as part of scaffolding it (separate task).
- Add dependencies outside the version catalog.
