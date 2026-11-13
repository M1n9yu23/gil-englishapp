# Code style

[한국어](code-style.ko.md)

Spotless formats the code; [build.md](build.md#before-a-pull-request) has the command.
The rules below are the ones a formatter cannot check. When in doubt, copy the shape of the nearest existing file.

## Packages and names

A module's package follows its Gradle path: `:feature:lesson:impl:data` is `com.gyugle.gil.feature.lesson.impl.data`.

Inside a module, code is grouped by what it is:

- `impl:domain`: `model/` and `repository/` (interfaces).
- `impl:data`: `di/`, `repository/`, and `dto/`, `mapper/`, `dao/`, `entity/` where the module needs them.
- `impl:presentation`: one package per screen, such as `player/` or `path/`, and `component/` for composables that several of its screens share.

An implementation is named after what it is built on: `RoomProgressRepository`, `DataStoreLessonRunRepository`, `AndroidTextToSpeechSpeaker`.
IDs are value classes, such as `LessonId`, not bare strings.
Keep declarations `internal` unless another module uses them.

## Screens

Each screen is a set of files with one prefix, as in `feature/lesson/impl/presentation/.../player/`:

| File | Holds |
|---|---|
| `LessonState` | everything the screen draws, as one data class |
| `LessonAction` | what the learner did, as a sealed interface: `OnCloseClick`, `OnCheckClick` |
| `LessonEvent` | one-off effects the screen carries out: closing, opening another screen, asking for a permission |
| `LessonViewModel` | `state: StateFlow<LessonState>`, `events: Flow<LessonEvent>` and `onAction(action)` |
| `LessonScreen.kt` | `LessonRoot`, which gets the ViewModel and handles events, and `LessonScreen(state, onAction, modifier)`, which only draws |

Something that must still be there after the screen rotates is state, not an event.
Previews and screenshot tests render `LessonScreen` with a state, so keep it free of ViewModels and Hilt.
The screen draws its own UI models (`StepUi`, `OptionUi`), mapped from the domain models in the presentation module.
Text that a ViewModel picks is a `UiText`, resolved in the composable.

## Errors

Return an `Outcome` for a failure the caller has to handle, and catch exceptions where they happen, in the data module.
After catching `Exception` inside a coroutine, call `currentCoroutineContext().ensureActive()` before anything else, so a cancellation is not swallowed. `ContentReads.kt` in `feature:curriculum:impl:data` shows the pattern.
An exception message never reaches the screen. Map the error to a string resource instead.

## Logging

Use Timber. Only debug builds plant a tree, so release builds log nothing.
Pass values as format arguments: `Timber.w("Could not speak a sentence: %s", error)`.
Do not log what the learner typed, said or recorded.

## Comments

Write a comment when the code cannot say why: a constraint, a trap, the reason for an unusual choice.

```kotlin
// A String, not LessonId: Dagger cannot inject a value class parameter.
@Assisted lessonId: String,
```

KDoc on a public declaration says what a caller needs to know to use it correctly, not what its name already says.
A dependency in `build.gradle.kts` that is not obvious gets a one-line comment that says why the module needs it.
Do not leave commented-out code or `TODO`s; open an issue instead.
Comments, KDoc and log messages are in English.
