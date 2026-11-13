# 코드 스타일

[English](code-style.md)

코드 서식은 Spotless가 맞춥니다. 명령은 [build.ko.md](build.ko.md#풀-리퀘스트-전에)에 있습니다.
아래는 서식 도구가 잡지 못하는 규칙입니다. 헷갈리면 가장 가까운 기존 파일의 모양을 따르세요.

## 패키지와 이름

모듈의 패키지는 Gradle 경로를 따릅니다. `:feature:lesson:impl:data`의 패키지는 `com.gyugle.gil.feature.lesson.impl.data`입니다.

모듈 안에서는 종류별로 나눕니다.

- `impl:domain`: `model/`, `repository/`(인터페이스).
- `impl:data`: `di/`, `repository/`, 그리고 필요할 때 `dto/`, `mapper/`, `dao/`, `entity/`.
- `impl:presentation`: 화면마다 패키지 하나(`player/`, `path/` 등), 여러 화면이 같이 쓰는 컴포저블은 `component/`, 모듈이 무언가를 바인딩하면 `di/`.

구현 클래스 이름 앞에는 무엇으로 만들었는지를 붙입니다(`RoomProgressRepository`, `DataStoreLessonRunRepository`, `AndroidTextToSpeechSpeaker`).
`feature:curriculum:api`의 콘텐츠 ID는 `LessonId`, `WordId` 같은 value class입니다.

## 화면

화면 하나는 같은 접두어를 쓴 파일 묶음입니다. `feature/lesson/impl/presentation/.../player/`가 예입니다.

| 파일 | 담는 것 |
|---|---|
| `LessonState` | 화면이 그리는 모든 것, data class 하나 |
| `LessonAction` | 학습자가 한 일, sealed interface: `OnCloseClick`, `OnCheckClick` |
| `LessonEvent` | 화면이 한 번 처리하는 일: 닫기, 다른 화면 열기, 권한 요청 |
| `LessonViewModel` | `state: StateFlow<LessonState>`, `events: Flow<LessonEvent>`, `onAction(action)` |
| `LessonScreen.kt` | ViewModel을 받아 이벤트를 처리하는 `LessonRoot`, 그리기만 하는 `LessonScreen(state, onAction, modifier)` |

화면을 돌린 뒤에도 남아 있어야 하는 것은 이벤트가 아니라 상태입니다.
미리보기와 스크린샷 테스트는 상태를 넣어 `Screen` 컴포저블을 그리므로, 여기에는 ViewModel과 Hilt가 들어오면 안 됩니다.
화면은 자기 UI 모델(`StepUi`, `OptionUi`)을 그리고, 도메인 모델을 이 모델로 바꾸는 일은 presentation 모듈에서 합니다.
스낵바 문구처럼 ViewModel이 보내는 메시지는 `UiText`로 두고, 실제 문자열은 컴포저블에서 꺼냅니다.

## 오류

호출하는 쪽이 처리해야 하는 실패는 `Outcome`으로 돌려주고, 예외는 생기는 곳인 data 모듈에서 잡습니다.
저장소의 `Flow`를 모으는 ViewModel은 `.catch {}`를 붙여서, 그 데이터 없이도 화면이 뜨게 합니다. `PathViewModel`이 그렇게 합니다.
코루틴 안에서 `Exception`을 잡았다면 다른 일을 하기 전에 `currentCoroutineContext().ensureActive()`를 불러서 취소를 삼키지 않게 합니다. `feature:curriculum:impl:data`의 `ContentReads.kt`가 그렇게 합니다.
예외 메시지는 화면에 내보내지 않습니다. 오류를 문자열 리소스로 바꿔서 보여 주세요.

## 로그

Timber를 씁니다. debug 빌드에서만 Tree를 심으므로 release 빌드에는 로그가 남지 않습니다.
값은 형식 인자로 넘깁니다: `Timber.w("Could not speak a sentence: %s", error)`.
학습자가 입력하거나 말하거나 녹음한 내용은 로그에 남기지 않습니다.

## 주석

코드만으로는 이유를 알 수 없을 때 주석을 씁니다. 제약, 함정, 흔하지 않은 선택의 이유 같은 것입니다.

```kotlin
// A String, not LessonId: Dagger cannot inject a value class parameter.
@Assisted lessonId: String,
```

공개 선언의 KDoc에는 이름만 봐서는 모르는, 제대로 쓰려면 알아야 할 것을 씁니다.
`build.gradle.kts`에서 이유가 바로 보이지 않는 의존에는 모듈이 왜 필요로 하는지 한 줄 주석을 답니다.
주석 처리한 코드나 `TODO`는 남기지 말고 이슈를 여세요.
주석, KDoc, 로그 메시지는 영어로 씁니다.
