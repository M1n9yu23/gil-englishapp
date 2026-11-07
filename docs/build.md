# Build

[한국어](build.ko.md)

Gil builds with Gradle and the Android Gradle plugin. You need Android Studio and the Android SDK. The JDK bundled with Android Studio is enough. The app uses no API keys or network services, so there is nothing to sign up for.

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

## Build types

| Type | Application ID | R8 | Signed with | Use |
|---|---|---|---|---|
| `debug` | `com.gyugle.gil.debug` | Off | Debug key | Day-to-day development |
| `staging` | `com.gyugle.gil.staging` | On | Debug key | Testing the release code on a device before shipping |
| `release` | `com.gyugle.gil` | On | Upload key, if configured | Google Play |

The three IDs differ, so all three install side by side. `staging` starts as a copy of `release` and falls back to `release` for any dependency that has no `staging` variant.

```
./gradlew :app:assembleStaging
./gradlew :app:bundleRelease
```

### Signing a release

Release builds are signed only when a `keystore.properties` file exists in the project root. Without it, `assembleRelease` produces an unsigned APK and everything else still builds. The file is ignored by git and looks like this:

```
KEYSTORE_PATH=/path/outside/the/repository/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=upload
KEY_PASSWORD=...
```

Keep the keystore itself outside the repository. Google Play holds the app signing key; this key is only the upload key.

### R8

R8 shrinks and obfuscates `staging` and `release` through the `optimization` block. Keep rules live in `app/src/main/keepRules/`. The only rule the app adds keeps line numbers so stack traces can be retraced with `app/build/outputs/mapping/release/mapping.txt`. Libraries bring their own rules.

## No INTERNET permission

Gil runs entirely offline, and the build enforces it. Every application module gets a `verify<Variant>NoInternetPermission` task that reads the merged manifest and fails if `android.permission.INTERNET` appears in it, whether it comes from the app or from a library. `./gradlew check` runs them all, and `./gradlew verifyNoInternetPermission` runs only these tasks.

## Convention plugins

Shared build setup is in `build-logic/convention`, an included build. A module's build file applies one or two of these plugins and then lists only its own dependencies.

| Plugin | Applied to | Adds |
|---|---|---|
| `gil.jvm.library` | Modules with no Android code: `core:domain`, feature `api` and `impl:domain` | Kotlin JVM, serialization, coroutines, unit tests |
| `gil.android.library` | Android library modules | Namespace from the module path, SDK levels, unit and device tests |
| `gil.android.library.compose` | Shared UI modules (`core:designsystem`, `core:presentation`, `core:navigation`) | The above plus Compose and Material 3 |
| `gil.android.feature` | `feature:<name>:impl:presentation` | The above plus Hilt, ViewModel support and the shared UI modules |
| `gil.android.application` | Application modules | Application ID, version and SDK levels from the catalog; the INTERNET permission check |
| `gil.android.application.compose` | `:app` | The above plus Compose and the Navigation 3 ViewModel decorator |
| `gil.hilt` | Any module with `@Inject` classes or Hilt modules | Hilt through KSP |
| `gil.room` | The module that owns a Room database | Room through KSP; schemas exported to `schemas/` |
| `gil.root` | The root project | A `check` task and the document translation check |

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

## Checks

Before opening a pull request, run what CI runs:

```
./gradlew spotlessCheck verifyDocTranslations lintDebug testDebugUnitTest assembleDebug verifyNoInternetPermission
```

### Formatting

Spotless formats every `.kt` and `.kts` file with ktlint in the `android_studio` style plus the Compose rules from compose-rules. It also adds the Apache 2.0 license header from `spotless/license-header.txt`. The rule settings are in `.editorconfig`:

- Material 2 APIs are not allowed.
- Collection parameters of composables must be stable types.
- Preview functions end in `Preview`.
- The only `CompositionLocal`s allowed are the design system tokens `LocalSpacing` and `LocalMotion`.

```
./gradlew spotlessApply   # fix
./gradlew spotlessCheck   # verify
```

### Lint

Android Lint fails the build on any warning. These checks are raised to errors because they guard text and accessibility: `HardcodedText`, `ContentDescription`, `SmallSp`, `SpUsage`, `TypographyEllipsis`, `TypographyDashes`, `UnusedResources` and `MissingTranslation`. Checks that only report newer dependency versions are off, since Dependabot handles updates. The app module runs with `checkDependencies`, so unused resources and missing translations are found across every module it uses.

To keep a warning on purpose, suppress it at the narrowest place, such as `tools:ignore` on one element or `@Suppress` on one declaration, and add a comment that says why.

### Document translations

Every public document exists in English (`name.md`) and Korean (`name.ko.md`), and each one links to the other within its first five lines. Issue forms pair the same way (`name.yml`, `name.ko.yml`). The pull request template holds both languages in one file. `LICENSE` and `NOTICE` are legal texts and stay in English only.

`./gradlew verifyDocTranslations` checks the pairs and the links. It looks only at files git does not ignore, and `./gradlew check` runs it too.

### Continuous integration

`.github/workflows/ci.yml` runs the commands above on every push to `main` and on every pull request. The workflow can only read the repository, and each action is pinned to a commit SHA. Dependabot (`.github/dependabot.yml`) proposes Gradle and GitHub Actions updates once a week. Kotlin and KSP come in one pull request because they must change together.

## Design decisions

- **No `kotlin-android` plugin.** AGP 9 compiles Kotlin itself, and applying the Kotlin Android plugin on top of it fails the build.
- **Plugin ids start with `gil.`, not a reverse domain.** The plugins are never published, so they only need to be unique inside this build.
- **Convention plugin entries in the catalog have no version.** The root project applies `gil.root`, which puts `build-logic` on the classpath of every module. Gradle then rejects a request for one of its plugins that names a version, even `unspecified`.
- **One Spotless setup at the root.** It covers every module and `build-logic` with a single list of targets, and writes LF line endings to match `.gitattributes`.
