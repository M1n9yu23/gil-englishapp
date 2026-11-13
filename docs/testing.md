# Testing

[한국어](testing.ko.md)

Unit tests use JUnit Jupiter, AssertK, Turbine and kotlinx-coroutines-test. The project uses no mocking library.
Tests that run on Robolectric, such as screenshot and Room tests, use JUnit 4, because Robolectric's runner is a JUnit 4 runner.

```
./gradlew testDebugUnitTest                                   # every module
./gradlew :feature:lesson:impl:presentation:testDebugUnitTest # one module
```

## Names

Name a test method `subject_condition_expectedResult`, or `subject_expectedResult` when there is no condition:

```kotlin
@Test
fun load_missingLesson_failsWithoutRetry() { ... }
```

A test class is named after the class it tests: `LessonViewModelTest`, `RoomProgressRepositoryTest`.

## Fakes

Replace a dependency with a fake, a small working implementation of its interface, named `Fake` plus what it replaces. One fake can implement a feature's observer and commands together, as `FakeGoalRepository` does for `GoalObserver` and `GoalCommands`.
A fake sits in the same package as the interface it implements.

- Put a fake that other modules' tests need in the `testFixtures` of the module that declares the interface, such as `FakeProgressObserver` in `feature:progress:api`. Other modules use it with `testImplementation(testFixtures(projects.feature.progress.api))`.
- A fake that only one module's tests use lives in that module's `src/test`, such as `FakeLessonRunRepository` in `feature:lesson:impl:presentation`.

## ViewModels

Set the main dispatcher before each test and reset it after:

```kotlin
@BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
@AfterEach fun tearDown() = Dispatchers.resetMain()
```

Build the ViewModel by hand with fakes, without Hilt. Read `state.value` for state and use Turbine's `test {}` on `events`.
`LessonViewModelTest` covers most of what a screen does.

## Storage

A repository over Room or DataStore is tested against the real storage, not a fake of it.
DataStore tests write to a temporary directory. Room tests run on Robolectric, with an in-memory database or, for the shipped course and for migrations, a database file.
A change to the progress database schema needs a test in `ProgressMigrationTest`.

## Screenshots

Screens and most design system components have Roborazzi tests that render on the JVM through Robolectric, so they need no device.
The reference images are committed in each module's `src/test/screenshots/`.

```
./gradlew verifyRoborazziDebug   # compare with the committed images
./gradlew recordRoborazziDebug   # write new images after a visual change
```

A screenshot test renders the stateless `Screen` composable inside `GilTheme` with a prepared state, as `LessonScreenshotTest` does. Use Robolectric qualifiers for the language, the window size and dark mode (`+night`).
Stop the Compose clock with `mainClock.autoAdvance = false` and move it past the animations before capturing, so every run gives the same image.
CI does not compare screenshots, so run `verifyRoborazziDebug` yourself when you change a screen, and look at every image that changed before you commit it.

## Device tests

Only code that needs a real device has instrumented tests: speech recognition, text-to-speech and recording in `core:speech`.
They are named `...DeviceTest` and run on a connected device or emulator. (`:baselineprofile` also runs on a device, but it builds the startup profile and is not a test suite.)

```
./gradlew :core:speech:connectedDebugAndroidTest
```
