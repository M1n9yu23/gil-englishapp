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

CI runs the same checks and a debug build on every pull request.
If you changed how a screen looks, also run `./gradlew recordRoborazziDebug` and commit the changed images in `src/test/screenshots/`.

## Rules the build enforces

- Lint treats warnings as errors, so `lintDebug` fails on any warning. To keep one on purpose, suppress it at the narrowest place and add a comment that says why.
- A string resource in `values/` without a Korean version in `values-ko/` fails lint. Lint does not catch text written straight into a composable, so put every user-facing string in resources.
- `.kt` and `.kts` files need the license header; `spotlessApply` adds it.
- The app must not request the INTERNET permission, from its own manifest or from a library.
- Learning content in `content/` must pass its checks. See [content.md](content.md).
