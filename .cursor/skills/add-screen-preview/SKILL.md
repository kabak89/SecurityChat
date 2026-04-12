---
name: add-screen-preview
description: >-
  Adds Jetpack Compose @Preview for a screen or reusable screen content in Kotlin
  Multiplatform feature-ui modules (SecurityChat): Gradle dependencies (commonMain
  preview annotation + androidMain tooling for Android Studio), AppTheme wrapper,
  emptyFlow for events, mock state. Use when the user asks for a preview, @Preview,
  Compose preview for a screen, or to mirror preview setup from settings/chats-ui.
---

# Превью экрана (Compose, KMP)

## Когда применять

- Новый или существующий экран / `*Content` в `features/*/*-ui`, нужно показать в **Android Studio
  Interactive Preview**.
- Аналог эталона: превью
  в [SettingsMainScreen.kt](features/settings/settings-ui/src/commonMain/kotlin/com/security/chat/multiplatform/features/settings/ui/screens/main/SettingsMainScreen.kt)
  или [ChatListScreen.kt](features/chats/chats-ui/src/commonMain/kotlin/com/security/chat/multiplatform/features/chats/ui/screens/chatlist/ChatListScreen.kt).

## 1. Зависимости в `build.gradle.kts` модуля `*-ui`

Превью в **commonMain** использует аннотацию `@Preview`; рендер в панели превью на Android тянет *
*полный tooling** только на `androidMain`.

**Проверить / добавить:**

| Source set    | Назначение                                          | Зависимость                                                                                                                                                           |
|---------------|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `commonMain`  | `@Preview`, `PreviewParameterProvider` (если нужно) | Обычно уже есть транзитивно через `api(projects.common.uiKit)` → `libs.ui.tooling.preview`. Если модуля нет в графе — явно: `implementation(libs.ui.tooling.preview)` |
| `androidMain` | Рендер превью в Android Studio                      | **Обязательно добавить**, если превью не подхватывается: `implementation(libs.androidx.ui.tooling)`                                                                   |

Пример блока для `androidMain` (как
в [chats-ui/build.gradle.kts](features/chats/chats-ui/build.gradle.kts)):

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
```

Каталог версий: [gradle/libs.versions.toml](gradle/libs.versions.toml) — алиасы `ui-tooling-preview`
и `androidx-ui-tooling` (в коде: `libs.ui.tooling.preview`, `libs.androidx.ui.tooling`).

## 2. Код превью

1. **Превью вызывает тот же composable, что и экран** (часто приватный `*ScreenContent` / `*Content`
   в том же файле — тогда не нужно менять `private` / `internal`).
2. Обернуть в **`AppTheme { ... }`** (см. [
   `Theme.kt`](common/ui-kit/src/commonMain/kotlin/com/security/chat/multiplatform/common/ui/kit/theme/Theme.kt),
   `common.ui.kit`).
3. **Состояние** — мок-данные из реальных типов state/feature entities (как в проде).
4. **Для `Flow` событий** — `events = emptyFlow()` и импорт `kotlinx.coroutines.flow.emptyFlow` (см.
   Settings).
5. **Колбэки** — пустые лямбды `{}` или `{ _ -> }` для параметров.
6. Сигнатура превью: `@Preview` + `@Composable` + `internal fun XxxPreview()` (как в проекте).

Минимальный шаблон:

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

## 3. Чеклист

- [ ] В `*-ui/build.gradle.kts`: при необходимости `commonMain` → `libs.ui.tooling.preview`; для
  Android — `androidMain` → `libs.androidx.ui.tooling` (`implementation`).
- [ ] Превью в том же файле, что и приватный контент, либо контент сделан `internal` и доступен из
  `androidMain` preview-only файла (в проекте предпочтительно **один файл**).
- [ ] `AppTheme`, мок state, `emptyFlow()` для side-effect flows.
- [ ] Сборка `:features:<feature>:<feature>-ui:compileKotlinIosArm64` (или целевой таргет) без
  ошибок.

## 4. Не делать

- Не подключать `libs.androidx.ui.tooling` в `commonMain` — это AndroidX-артефакт для JVM/Android.
- Не дублировать тяжёлую логику ViewModel/Koin в превью — только UI-слой с моками.
