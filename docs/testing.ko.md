# 테스트

[English](testing.md)

단위 테스트에는 JUnit Jupiter, AssertK, Turbine, kotlinx-coroutines-test를 씁니다. 목(mock) 라이브러리는 쓰지 않습니다.
스크린샷 테스트나 Room 테스트처럼 Robolectric에서 도는 테스트는 JUnit 4를 씁니다. Robolectric의 러너가 JUnit 4 러너이기 때문입니다.

```
./gradlew testDebugUnitTest                                   # 모든 모듈
./gradlew :feature:lesson:impl:presentation:testDebugUnitTest # 모듈 하나
```

## 이름

테스트 메서드 이름은 `대상_조건_기대결과` 순서이고, 조건이 없으면 `대상_기대결과`로 씁니다.

```kotlin
@Test
fun load_missingLesson_failsWithoutRetry() { ... }
```

테스트 클래스 이름은 테스트하는 클래스 이름을 따릅니다: `LessonViewModelTest`, `RoomProgressRepositoryTest`.

## Fake

의존하는 것은 Fake로 바꿉니다. Fake는 인터페이스를 간단하지만 실제로 동작하게 구현한 클래스이고, 이름은 `Fake` 뒤에 인터페이스 이름을 붙입니다.

- `api` 계약의 Fake는 그 모듈의 `testFixtures`에 둡니다. `feature:progress:api`의 `FakeProgressObserver`가 그렇습니다. 다른 모듈은 `testImplementation(testFixtures(projects.feature.progress.api))`로 가져다 씁니다.
- 한 모듈의 테스트만 쓰는 Fake는 그 모듈의 `src/test`에 둡니다. `feature:lesson:impl:presentation`의 `FakeLessonRunRepository`가 그 예입니다.

## ViewModel

테스트마다 main 디스패처를 바꾸고, 끝나면 되돌립니다.

```kotlin
@BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
@AfterEach fun tearDown() = Dispatchers.resetMain()
```

ViewModel은 Hilt 없이 Fake를 넣어 직접 만듭니다. 상태는 `state.value`로 읽고, `events`는 Turbine의 `test {}`로 확인합니다.
화면이 하는 일 대부분을 다루는 예로는 `LessonViewModelTest`를 보세요.

## 저장소

저장소 구현은 Fake 대신 실제 저장 방식으로 테스트합니다.
DataStore 테스트는 JUnit `@TempDir`에 파일을 씁니다. Room 테스트는 Robolectric에서 메모리 데이터베이스로 돌립니다.
학습 기록 데이터베이스의 스키마를 바꾸면 `ProgressMigrationTest`에 테스트를 더해야 합니다.

## 스크린샷

화면과 디자인 시스템 컴포넌트에는 Roborazzi 테스트가 있습니다. Robolectric으로 JVM에서 그리므로 기기가 필요 없습니다.
기준 이미지는 모듈마다 `src/test/screenshots/`에 커밋되어 있습니다.

```
./gradlew verifyRoborazziDebug   # 커밋된 이미지와 비교
./gradlew recordRoborazziDebug   # 화면을 바꾼 뒤 새 이미지 저장
```

스크린샷 테스트는 `LessonScreenshotTest`처럼 상태를 미리 만들어 상태 없는 `Screen` 컴포저블을 `GilTheme` 안에서 그립니다. 언어, 창 크기, 다크 모드(`+night`)는 Robolectric qualifier로 정합니다.
CI는 스크린샷을 비교하지 않습니다. 화면을 바꿨다면 `verifyRoborazziDebug`를 직접 돌리고, 바뀐 이미지를 하나씩 열어 본 뒤에 커밋하세요.

## 기기 테스트

실제 기기가 있어야 하는 코드만 계측 테스트를 둡니다. `core:speech`의 음성 인식, 음성 합성, 녹음이 그렇습니다.
이름은 `...DeviceTest`이고, 연결된 기기나 에뮬레이터에서 돌립니다.

```
./gradlew :core:speech:connectedDebugAndroidTest
```
