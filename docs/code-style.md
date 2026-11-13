# Code style

[한국어](code-style.ko.md)

Spotless formats the code and adds the license header; [build.md](build.md#before-a-pull-request) has the command.
The rules below are the ones a formatter cannot check. When in doubt, copy the shape of the nearest existing file.

## Packages

A feature or core module's package follows its Gradle path: `:feature:lesson:impl:data` is `com.gyugle.gil.feature.lesson.impl.data`. `:app` uses `com.gyugle.gil`.

Inside a module, a package groups files of one kind once there are several of them:

- `di/` holds a module's Hilt modules.
- `model/` holds a domain module's models; small domain modules keep their rules and interfaces in the root package.
- A data module uses `repository/`, `dto/`, `mapper/`, `dao/` and `entity/`, or a package named after its subject, such as `backup/` or `playback/`.
- A presentation module has one package per screen, such as `player/` or `path/`, and `component/` for composables that several of its screens share.

## Names

Name a class after what it does. The project has no `Base`, `Abstract`, `Manager`, `Helper`, `Util` or `Impl` classes.
An implementation is named after what it is built on: `RoomProgressRepository`, `DataStoreLessonRunRepository`, `AndroidTextToSpeechSpeaker`.

## Choosing a type

| Use | When | Example |
|---|---|---|
| interface | a capability or contract that tests replace with a fake | `Speaker`, `CourseObserver`, `LessonRunRepository` |
| sealed interface | a closed set of kinds that `when` must cover | `LessonStep`, `StudyItemId`, every Action and Event |
| marker interface | only a type bound is needed | `RootError` |
| `@JvmInline value class` | a raw value that must not be mixed up with another | `LessonId`, `WordId`, `StudyDay` |
| enum | a closed set of plain values | `ReviewRating`, `Accent`, `ThemeMode` |
| data class | a value model | public models, most screen states |
| abstract class | only when a library requires one | the Room databases |
| object | stateless constants or a single implementation | `GilIcons` |

## Android services

ViewModels have no `Context` and do not call Android services. They reach speech, recording, reminders and backup files through interfaces such as `Speaker`, `VoiceRecorder`, `ReminderCommands` and `BackupRepository`, which tests replace with fakes.
These interfaces speak in the project's own types, with no Android callbacks or integer constants. Turn a callback into a `Flow` with `callbackFlow`, or into a `suspend` function with `suspendCancellableCoroutine`.
Composables may still open system screens and pickers themselves, such as the file picker in `BackupScreen`.
Room, DataStore and Compose are used directly.

## Screens

A screen has a State, an Action and an Event type, a ViewModel and a screen file, all with one prefix. The lesson screen in `feature/lesson/impl/presentation/.../player/` keeps each in its own file; smaller screens such as `ListeningState.kt` declare Action and Event next to the State.

| Type | Holds |
|---|---|
| `LessonState` | everything the screen draws: a data class, or a sealed interface when the whole screen is either loading, failed or ready, as `TodayState` is. `LessonState` keeps its phases in a sealed `LessonPhase` next to fields every phase shares, such as the title |
| `LessonAction` | what the learner did, as a sealed interface: `OnCloseClick`, `OnCheckClick` |
| `LessonEvent` | one-off effects the screen carries out: closing, opening another screen, asking for a permission |
| `LessonViewModel` | `state: StateFlow<LessonState>`, `events: Flow<LessonEvent>` and `onAction(action)` |
| `LessonScreen.kt` | `LessonRoot`, which gets the ViewModel and handles events, and `LessonScreen(state, onAction, modifier)`, which only draws |

Something that must still be there after the screen rotates is state, not an event.
Previews and screenshot tests render the `Screen` composable with a state, so keep it free of ViewModels and Hilt.
The screen draws its own UI models (`StepUi`, `OptionUi`), mapped from the domain models in the presentation module.
A message the ViewModel sends, such as the text of a snackbar, is a `UiText` and is resolved in the composable.

## Errors and coroutines

Return an `Outcome` for a failure the caller has to handle. Catch an exception close to where it is thrown and turn it into an `Outcome` or a fallback that the screen can show.
A `Flow` from the Room databases can throw, so a ViewModel guards it: `.catch {}` on a flow it keeps collecting, as in `PathViewModel`, or `try`/`catch` around a single `.first()`, as in `ReviewViewModel`. `dataOrEmpty()` already handles read errors for DataStore flows.
After catching `Exception` inside a coroutine, call `currentCoroutineContext().ensureActive()` before anything else, so a cancellation is not swallowed. `ContentReads.kt` in `feature:curriculum:impl:data` shows the pattern.
An exception message never reaches the screen. Map the error to a string resource instead.
A write that must finish even if the learner leaves the screen runs in `withContext(NonCancellable)`, as `LessonViewModel` does when it saves a finished lesson.
Work started outside a screen, such as in a broadcast receiver or a service, runs in the `@ApplicationScope` `CoroutineScope`.

## Logging

Use Timber. Only debug builds plant a tree, so release builds log nothing.
A message is one short English sentence with no full stop, and values go in as format arguments: `Timber.w("Could not speak a sentence: %s", error)`.
Never log what the learner typed, said or recorded, or the contents of their files.

## Comments and KDoc

A comment says what the code cannot: why it is written this way, a constraint, or a trap. Write one in these places:

- Code that looks wrong or roundabout without the reason:
  ```kotlin
  // A String, not LessonId: Dagger cannot inject a value class parameter.
  @Assisted lessonId: String,
  ```
- A suppressed lint warning: say why it is safe, as above `@SuppressLint("MissingPermission")` in `AudioRecordVoiceRecorder.kt`.
- A dependency in `build.gradle.kts` whose reason is not obvious: one line on why the module needs it.
- A token value that needs a reason, such as `PathTokens.LineCenter`.
- A string resource whose translator needs context, such as where it appears or what a placeholder holds.

Write KDoc for public and internal declarations whose behaviour the signature does not show: when a value is null, the order of a list, whether a range includes its ends, when a `Flow` emits again, units, and what happens on failure. `ProgressObserver` is a good model.
Skip KDoc when the name says it all, as for `ProgressObserver.observeCompletedLessons()`.

- A function's KDoc starts with a verb: "Runs [block] on the content database.", "Writes with [transform] and reports a failed write as …".
- A class or property's KDoc usually starts with what it is: "One row of a list.", "The current moment and the [StudyDay] it falls on." An interface for one capability can start with what it does, as `ProgressObserver` does: "Reads the learner's record."
- Refer to parameters and other declarations as `[name]`. Use `@param` only when a parameter needs more than its name.

Do not write comments that repeat the name or narrate the code, section comments such as `// region`, commented-out code, or `TODO`s (open an issue instead).
Describe the code as it is now. The history of a change belongs in the commit message.
Comments, KDoc and log messages are in English.
