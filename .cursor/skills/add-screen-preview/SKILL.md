---
name: add-screen-preview
description: >-
  Adds Jetpack Compose @Preview for a screen or reusable screen content in Kotlin
  Multiplatform feature-ui modules (SecurityChat): Gradle dependencies (commonMain
  preview annotation + androidMain tooling for Android Studio), AppTheme wrapper,
  emptyFlow for events, mock state. Use when the user asks for a preview, @Preview,
  Compose preview for a screen, or to mirror preview setup from settings/chats-ui.
---

# Screen preview (Compose, KMP)

## When to use

- A new or existing screen / `*Content` in `features/*/*-ui` that needs to be shown in **Android
  Studio
  Interactive Preview**.
- Reference analog: the preview
  in [SettingsMainScreen.kt](features/settings/settings-ui/src/commonMain/kotlin/com/security/chat/multiplatform/features/settings/ui/screens/main/SettingsMainScreen.kt)
  or [ChatListScreen.kt](features/chats/chats-ui/src/commonMain/kotlin/com/security/chat/multiplatform/features/chats/ui/screens/chatlist/ChatListScreen.kt).

## 1. Dependencies in the `*-ui` module `build.gradle.kts`

A preview in **commonMain** uses the `@Preview` annotation; rendering in the preview panel on
Android
pulls in the **full tooling** only on `androidMain`.

**Check / add:**

| Source set    | Purpose                                            | Dependency                                                                                                                                                                                   |
|---------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `commonMain`  | `@Preview`, `PreviewParameterProvider` (if needed) | Usually already present transitively via `api(projects.common.uiKit)` → `libs.ui.tooling.preview`. If the module is not in the graph — explicitly: `implementation(libs.ui.tooling.preview)` |
| `androidMain` | Preview rendering in Android Studio                | **Must be added** if the preview is not picked up: `implementation(libs.androidx.ui.tooling)`                                                                                                |

Example block for `androidMain` (as
in [chats-ui/build.gradle.kts](features/chats/chats-ui/build.gradle.kts)):

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
```

Version catalog: [gradle/libs.versions.toml](gradle/libs.versions.toml) — aliases
`ui-tooling-preview`
and `androidx-ui-tooling` (in code: `libs.ui.tooling.preview`, `libs.androidx.ui.tooling`).

## 2. Preview code

1. **The preview calls the same composable as the screen** (often a private `*ScreenContent` /
   `*Content` in the same file — then there is no need to change `private` / `internal`).
2. Wrap in **`AppTheme { ... }`** (see [
   `Theme.kt`](common/ui-kit/src/commonMain/kotlin/com/security/chat/multiplatform/common/ui/kit/theme/Theme.kt),
   `common.ui.kit`).
3. **State** — mock data from the real state/feature entity types (as in production).
4. **For `Flow` events** — `events = emptyFlow()` and import `kotlinx.coroutines.flow.emptyFlow` (
   see
   Settings).
5. **Callbacks** — empty lambdas `{}` or `{ _ -> }` for parameters.
6. Preview signature: `@Preview` + `@Composable` + `internal fun XxxPreview()` (as in the project).

Minimal template:

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.emptyFlow

@Preview
@Composable
internal fun MyScreenContentPreview() {
    AppTheme {
        MyScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = /* ... */,
            events = emptyFlow(),
            /* onClick = {}, ... */
        )
    }
}
```

## 3. Checklist

- [ ] In `*-ui/build.gradle.kts`: if needed, `commonMain` → `libs.ui.tooling.preview`; for
  Android — `androidMain` → `libs.androidx.ui.tooling` (`implementation`).
- [ ] The preview is in the same file as the private content, or the content is made `internal` and
  reachable from an `androidMain` preview-only file (one file is preferred in this project).
- [ ] `AppTheme`, mock state, `emptyFlow()` for side-effect flows.
- [ ] Build `:features:<feature>:<feature>-ui:compileKotlinIosArm64` (or the target you need)
  without
  errors.

## 4. Do not

- Do not add `libs.androidx.ui.tooling` to `commonMain` — it is an AndroidX artifact for
  JVM/Android.
- Do not duplicate heavy ViewModel/Koin logic in the preview — only the UI layer with mocks.
