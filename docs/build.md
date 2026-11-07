# Build

[한국어](build.ko.md)

```
./gradlew :app:installDebug     # day-to-day development
./gradlew :app:installStaging   # release code with R8, debug-signed, installs next to debug
```

Use `staging` to check a change that might behave differently after R8.

## Before a pull request

```
./gradlew spotlessApply
./gradlew spotlessCheck lintDebug testDebugUnitTest verifyNoInternetPermission
```

CI runs the second line on every pull request.

## Rules the build enforces

- Lint warnings fail the build. To keep one on purpose, suppress it at the narrowest place and add a comment that says why.
- Every user-facing string needs an English (`values/`) and a Korean (`values-ko/`) version.
- Kotlin files need the license header; `spotlessApply` adds it.
- The app must not request the INTERNET permission, from its own manifest or from a library.
