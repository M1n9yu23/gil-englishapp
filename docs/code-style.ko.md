# 코드 스타일

[English](code-style.md)

서식과 라이선스 머리말은 Spotless가 맞춥니다. 명령은 [build.ko.md](build.ko.md#풀-리퀘스트-전에)에 있습니다.
아래는 서식 도구가 잡지 못하는 규칙입니다. 헷갈리면 가장 가까운 기존 파일의 모양을 따르세요.

## 패키지

기능 모듈과 core 모듈의 패키지는 Gradle 경로를 따릅니다. `:feature:lesson:impl:data`의 패키지는 `com.gyugle.gil.feature.lesson.impl.data`입니다. `:app`은 `com.gyugle.gil`을 씁니다.

모듈 안에서는 같은 종류의 파일이 여럿 생기면 패키지로 묶습니다.

- `di/`에는 모듈의 Hilt 모듈을 둡니다.
- `model/`에는 domain 모듈의 모델을 둡니다. 작은 domain 모듈은 규칙과 인터페이스를 루트 패키지에 둡니다.
- data 모듈은 `repository/`, `dto/`, `mapper/`, `dao/`, `entity/`를 쓰거나, `backup/`, `playback/`처럼 다루는 대상의 이름으로 패키지를 만듭니다.
- presentation 모듈은 화면마다 패키지 하나(`player/`, `path/` 등)를 두고, 여러 화면이 같이 쓰는 컴포저블은 `component/`에 둡니다.

## 이름

클래스 이름은 하는 일로 짓습니다. 이 프로젝트에는 `Base`, `Abstract`, `Manager`, `Helper`, `Util`, `Impl`이 붙은 클래스가 없습니다.
구현 클래스 이름 앞에는 무엇으로 만들었는지를 붙입니다(`RoomProgressRepository`, `DataStoreLessonRunRepository`, `AndroidTextToSpeechSpeaker`).

## 타입 고르기

| 쓰는 것 | 언제 | 예 |
|---|---|---|
| interface | 테스트에서 Fake로 바꿔 끼우는 능력이나 계약 | `Speaker`, `CourseObserver`, `LessonRunRepository` |
| sealed interface | `when`이 빠짐없이 다뤄야 하는 닫힌 종류 | `LessonStep`, `StudyItemId`, 모든 Action과 Event |
| 마커 interface | 타입 제약만 필요할 때 | `RootError` |
| `@JvmInline value class` | 다른 값과 섞이면 안 되는 원시값 | `LessonId`, `WordId`, `StudyDay` |
| enum | 값만 있는 닫힌 집합 | `ReviewRating`, `Accent`, `ThemeMode` |
| data class | 값 모델 | 공개 모델, 대부분의 화면 상태 |
| abstract class | 라이브러리가 요구할 때만 | Room 데이터베이스 |
| object | 상태 없는 상수 묶음이나 구현이 하나뿐인 것 | `GilIcons` |

## Android 서비스

ViewModel은 `Context`를 갖지 않고 Android 서비스를 부르지 않습니다. 음성, 녹음, 알림 예약, 백업 파일은 `Speaker`, `VoiceRecorder`, `ReminderCommands`, `BackupRepository` 같은 인터페이스로 다루고, 테스트에서는 이 인터페이스를 Fake로 바꿉니다.
인터페이스는 이 프로젝트의 타입으로만 말하고, Android 콜백이나 정수 상수를 내보내지 않습니다. 콜백은 `callbackFlow`로 `Flow`를 만들거나 `suspendCancellableCoroutine`으로 `suspend` 함수를 만들어 바꿉니다.
시스템 화면이나 선택기는 컴포저블이 직접 열어도 됩니다. `BackupScreen`의 파일 선택기가 그렇습니다.
Room, DataStore, Compose는 감싸지 않고 바로 씁니다.

## 화면

화면 하나에는 State, Action, Event 타입과 ViewModel, 화면 파일이 있고, 모두 같은 접두어를 씁니다. `feature/lesson/impl/presentation/.../player/`의 수업 화면은 이것을 파일마다 나눴고, `ListeningState.kt`처럼 작은 화면은 Action과 Event를 State 옆에 함께 선언합니다.

| 타입 | 담는 것 |
|---|---|
| `LessonState` | 화면이 그리는 모든 것. data class이고, `TodayState`처럼 화면 전체가 불러오는 중·실패·준비됨 중 하나이면 sealed interface. `LessonState`는 제목처럼 모든 단계가 함께 쓰는 값 곁에 단계를 sealed `LessonPhase`로 둡니다 |
| `LessonAction` | 학습자가 한 일, sealed interface: `OnCloseClick`, `OnCheckClick` |
| `LessonEvent` | 화면이 한 번 처리하는 일: 닫기, 다른 화면 열기, 권한 요청 |
| `LessonViewModel` | `state: StateFlow<LessonState>`, `events: Flow<LessonEvent>`, `onAction(action)` |
| `LessonScreen.kt` | ViewModel을 받아 이벤트를 처리하는 `LessonRoot`, 그리기만 하는 `LessonScreen(state, onAction, modifier)` |

화면을 돌린 뒤에도 남아 있어야 하는 것은 이벤트가 아니라 상태입니다.
미리보기와 스크린샷 테스트는 상태를 넣어 `Screen` 컴포저블을 그리므로, 여기에는 ViewModel과 Hilt가 들어오면 안 됩니다.
화면은 자기 UI 모델(`StepUi`, `OptionUi`)을 그리고, 도메인 모델을 이 모델로 바꾸는 일은 presentation 모듈에서 합니다.
스낵바 문구처럼 ViewModel이 보내는 메시지는 `UiText`로 두고, 실제 문자열은 컴포저블에서 꺼냅니다.

## 오류와 코루틴

호출하는 쪽이 처리해야 하는 실패는 `Outcome`으로 돌려줍니다. 예외는 생기는 곳 가까이에서 잡아, `Outcome`이나 화면이 보여 줄 수 있는 대체 값으로 바꿉니다.
Room 데이터베이스의 `Flow`는 예외를 던질 수 있으므로 ViewModel이 막아 둡니다. 계속 모으는 흐름에는 `PathViewModel`처럼 `.catch {}`를 붙이고, `.first()`로 한 번 읽을 때는 `ReviewViewModel`처럼 `try`/`catch`로 감쌉니다. DataStore 흐름의 읽기 오류는 `dataOrEmpty()`가 이미 처리합니다.
코루틴 안에서 `Exception`을 잡았다면 다른 일을 하기 전에 `currentCoroutineContext().ensureActive()`를 불러서 취소를 삼키지 않게 합니다. `feature:curriculum:impl:data`의 `ContentReads.kt`가 그렇게 합니다.
예외 메시지는 화면에 내보내지 않습니다. 오류를 문자열 리소스로 바꿔서 보여 주세요.
학습자가 화면을 떠나도 끝까지 해야 하는 쓰기는 `withContext(NonCancellable)` 안에서 합니다. `LessonViewModel`이 끝낸 수업을 저장할 때가 그렇습니다.
브로드캐스트 리시버나 서비스처럼 화면 밖에서 시작한 일은 `@ApplicationScope` `CoroutineScope`에서 돌립니다.

## 로그

Timber를 씁니다. debug 빌드에서만 Tree를 심으므로 release 빌드에는 로그가 남지 않습니다.
메시지는 마침표 없는 짧은 영어 문장 하나이고, 값은 형식 인자로 넘깁니다: `Timber.w("Could not speak a sentence: %s", error)`.
학습자가 입력하거나 말하거나 녹음한 내용, 학습자 파일의 내용은 절대 로그에 남기지 않습니다.

## 주석과 KDoc

주석에는 코드만으로는 알 수 없는 것을 씁니다. 왜 이렇게 짰는지, 지켜야 할 제약, 빠지기 쉬운 함정 같은 것입니다. 주석이 있어야 하는 곳은 이렇습니다.

- 이유를 모르면 틀렸거나 돌아가는 것처럼 보이는 코드:
  ```kotlin
  // A String, not LessonId: Dagger cannot inject a value class parameter.
  @Assisted lessonId: String,
  ```
- lint 경고를 억제한 곳: 왜 괜찮은지 씁니다. `AudioRecordVoiceRecorder.kt`의 `@SuppressLint("MissingPermission")` 위 주석이 예입니다.
- `build.gradle.kts`에서 이유가 바로 보이지 않는 의존: 모듈이 왜 필요로 하는지 한 줄.
- 이유가 필요한 토큰 값: `PathTokens.LineCenter` 같은 것.
- 번역하는 사람에게 맥락이 필요한 문자열 리소스: 어디에 나오는지, 자리표시자에 무엇이 들어가는지.

public과 internal 선언 가운데 시그니처만으로는 동작이 드러나지 않는 것에는 KDoc을 씁니다. 값이 언제 null인지, 목록이 어떤 순서인지, 범위가 양 끝을 포함하는지, `Flow`가 언제 다시 내보내는지, 단위, 실패하면 어떻게 되는지 같은 것입니다. `ProgressObserver`가 좋은 본보기입니다.
`ProgressObserver.observeCompletedLessons()`처럼 이름으로 다 알 수 있으면 KDoc을 쓰지 않습니다.

- 함수 KDoc은 동사로 시작합니다: "Runs [block] on the content database.", "Writes with [transform] and reports a failed write as …".
- 클래스나 속성 KDoc은 대개 무엇인지를 밝히는 말로 시작합니다: "One row of a list.", "The current moment and the [StudyDay] it falls on." 능력 하나를 나타내는 인터페이스는 `ProgressObserver`의 "Reads the learner's record."처럼 하는 일로 시작해도 됩니다.
- 매개변수나 다른 선언은 `[name]`으로 가리킵니다. `@param`은 이름 말고 더 설명할 것이 있을 때만 씁니다.

이름을 되풀이하거나 코드를 풀어 쓰는 주석, `// region` 같은 구역 주석, 주석 처리한 코드, `TODO`는 쓰지 않습니다(할 일은 이슈로 여세요).
주석은 지금의 코드를 설명합니다. 무엇을 바꿨는지는 커밋 메시지에 씁니다.
주석, KDoc, 로그 메시지는 영어로 씁니다.
