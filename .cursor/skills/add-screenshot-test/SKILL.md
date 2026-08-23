---
name: add-screenshot-test
description: >-
  Adds a screenshot test for a Compose Multiplatform component or screen. 
  Automates Gradle setup (convention plugin, coreTest dependency) and generates 
  the test class in jvmTest following the project's naming conventions.
---

# Screenshot Test (Compose Multiplatform)

## When to use

- After creating or modifying a UI component or screen that has a `@Preview` function.
- Requirement: every UI component MUST have a corresponding screenshot test (see AGENTS.md).

## 1. Module Configuration (`build.gradle.kts`)

Ensure the `*-ui` module is configured for screenshot testing.

```kotlin
plugins {
    id("securitychat.convention.base")
    id("securitychat.convention.screenshot") // 1. Add this plugin
    // ...
}

kotlin {
    sourceSets {
        // ...
        jvmTest.dependencies {
            implementation(projects.common.coreTest) // 2. Add this dependency
        }
    }
}
```

## 2. Test File Location

- The test must be in the `jvmTest` source set.
- Package must mirror the component's package.
- File name: `<ComponentName>Test.kt` (e.g., if component is in `MyButton.kt`, test is
  `MyButtonTest.kt`).

## 3. Test Code Structure

The test should inherit from `ScreenshotTestBase` and call the existing `internal` preview
functions.

```kotlin
package com.security.chat.multiplatform.features.feature_name.ui

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class MyComponentTest : ScreenshotTestBase() {

    @Test
    fun myComponentPreview() {
        runScreenshotTest(screenshotName = "MyComponentPreview") {
            MyComponentPreview()
        }
    }
}
```

## 4. Execution

- **Record** (generate reference images):
  `./gradlew :features:feature-name:feature-name-ui:recordScreenshots`
- **Verify** (compare against references):
  `./gradlew :features:feature-name:feature-name-ui:checkScreenshots`

## 5. Checklist

- [ ] `securitychat.convention.screenshot` plugin applied.
- [ ] `projects.common.coreTest` added to `jvmTest`.
- [ ] Test class name matches component file name + `Test`.
- [ ] Test method name and `screenshotName` match the preview function name.
- [ ] Only use existing preview functions (do not create new ones just for the test).
- [ ] Reference screenshot recorded and committed to VCS.
