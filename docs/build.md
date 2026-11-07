# Build

Gil builds with Gradle and the Android Gradle plugin. You need Android Studio (its bundled JDK is enough) and the Android SDK. Nothing else: the app has no API keys, no network calls and no services to sign up for.

```
./gradlew :app:assembleDebug
```

## Versions

Every library, plugin and version is in `gradle/libs.versions.toml`. Build files refer to entries by alias (`libs.androidx.room.runtime`) and never write a version themselves.

The app's own settings live in the same file, as version keys with a `project` prefix:

| Key | Meaning |
|---|---|
| `projectApplicationId` | Application ID on Google Play |
| `projectVersionName` | Version shown to people, semantic (`0.1.0`) |
| `projectVersionCode` | Increases by one for every release |
| `projectMinSdkVersion` | Lowest Android version the app installs on |
| `projectTargetSdkVersion`, `projectCompileSdkVersion` | Android version the app is built and tested against |

Bytecode targets Java 17.

## Convention plugins

Shared build setup is in `build-logic/convention`, an included build. A module's build file applies one or two of these plugins and then lists only its own dependencies.

| Plugin | Applied to | Adds |
|---|---|---|
| `gil.jvm.library` | Modules with no Android code: `core:domain`, feature `api` and `impl:domain` | Kotlin JVM, serialization, coroutines, unit tests |
| `gil.android.library` | Android library modules | Namespace from the module path, SDK levels, unit and device tests |
| `gil.android.library.compose` | Shared UI modules (`core:designsystem`, `core:presentation`, `core:navigation`) | The above plus Compose and Material 3 |
| `gil.android.feature` | `feature:<name>:impl:presentation` | The above plus Hilt, ViewModel support and the shared UI modules |
| `gil.android.application` | Application modules | Application ID, version and SDK levels from the catalog |
| `gil.android.application.compose` | `:app` | The above plus Compose and the Navigation 3 ViewModel decorator |
| `gil.hilt` | Any module with `@Inject` classes or Hilt modules | Hilt through KSP |
| `gil.room` | The module that owns a Room database | Room through KSP; schemas exported to `schemas/` |

Each plugin is registered in `build-logic/convention/build.gradle.kts` with the id from the catalog, and modules apply it by alias:

```kotlin
plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}
```

## Tests

Unit tests run on JUnit 6 (Jupiter) with AssertK, Turbine and `kotlinx-coroutines-test`, added to every module by the plugins above.

```
./gradlew :<module>:testDebugUnitTest   # Android module
./gradlew :<module>:test                # JVM module
```

Device tests go in `src/androidTest`. A module without that directory builds no test APK and gets no device test dependencies.

## Design decisions

- **No `kotlin-android` plugin.** AGP 9 compiles Kotlin itself, and applying the Kotlin Android plugin on top of it fails the build.
- **Plugin ids start with `gil.`, not a reverse domain.** The plugins are never published, so they only need to be unique inside this build.
