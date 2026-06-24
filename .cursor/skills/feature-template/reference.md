# Reference tree of features/settings

Sources and Gradle (without `build/`). Use as a guide for replicating the directory structure.

```
features/settings/
├── settings-component-api/build.gradle.kts
├── settings-component-api/src/commonMain/kotlin/.../component/api/
│   ├── SettingsComponent.kt
│   ├── SettingsMainComponent.kt
│   └── ThemeComponent.kt
├── settings-component/build.gradle.kts
├── settings-component/src/commonMain/kotlin/.../component/
│   ├── SettingsComponent.kt
│   ├── SettingsMainComponent.kt
│   └── ThemeComponent.kt
├── settings-data-storage/build.gradle.kts
├── settings-data-storage/src/commonMain/kotlin/.../data/storage/
│   ├── SettingsStorage.kt
│   ├── di/SettingsDataStorageModule.kt
│   ├── entity/ThemeSM.kt
│   └── mapper/
├── settings-data/build.gradle.kts
├── settings-data/src/commonMain/kotlin/.../data/
│   ├── SettingsRepoImpl.kt
│   ├── di/SettingsDataModule.kt
│   └── mapper/SettingsDataMapper.kt
├── settings-domain/build.gradle.kts
├── settings-domain/src/commonMain/kotlin/.../domain/
│   ├── SettingsModel.kt
│   ├── di/SettingsDomainModule.kt
│   ├── entity/
│   └── repo/SettingsRepo.kt
└── settings-ui/build.gradle.kts
    └── src/commonMain/kotlin/.../ui/
        ├── di/SettingsUiModule.kt
        └── screens/
            ├── root/SettingsRootScreen.kt
            ├── main/
            └── theme/
```

Kotlin package: `com.security.chat.multiplatform.features.settings.<...>`.
