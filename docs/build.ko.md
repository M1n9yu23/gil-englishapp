# 빌드

[English](build.md)

Gil은 Gradle과 Android Gradle 플러그인으로 빌드합니다. Android Studio와 Android SDK만 있으면 되고, JDK는 Android Studio에 들어 있는 것을 그대로 씁니다. 앱이 API 키나 네트워크를 쓰지 않으니 따로 가입할 서비스도 없습니다.

```
./gradlew :app:assembleDebug
```

## 버전

라이브러리, 플러그인, 버전은 모두 `gradle/libs.versions.toml`에 있습니다. 빌드 파일은 이 파일의 별칭(`libs.androidx.room.runtime`)으로 항목을 가리키고, 버전 번호를 직접 적지 않습니다.

앱 자체 설정도 같은 파일에 있습니다. 이름이 `project`로 시작하는 버전 키입니다.

| 키 | 뜻 |
|---|---|
| `projectApplicationId` | Google Play에 등록하는 애플리케이션 ID |
| `projectVersionName` | 사용자에게 보이는 버전. 유의적 버전 형식(`0.1.0`) |
| `projectVersionCode` | 출시할 때마다 1씩 올리는 번호 |
| `projectMinSdkVersion` | 앱을 설치할 수 있는 가장 낮은 Android 버전 |
| `projectTargetSdkVersion`, `projectCompileSdkVersion` | 앱을 빌드하고 시험하는 기준 Android 버전 |

바이트코드 대상은 Java 17입니다.

## 빌드 타입

| 타입 | 애플리케이션 ID | R8 | 서명 키 | 용도 |
|---|---|---|---|---|
| `debug` | `com.gyugle.gil.debug` | 끔 | 디버그 키 | 평소 개발 |
| `staging` | `com.gyugle.gil.staging` | 켬 | 디버그 키 | 출시 전에 릴리스 코드를 기기에서 시험 |
| `release` | `com.gyugle.gil` | 켬 | 업로드 키(설정한 경우) | Google Play |

세 ID가 모두 다르기 때문에 한 기기에 세 앱을 함께 설치할 수 있습니다. `staging`은 `release` 설정을 복사해서 시작하고, `staging` 변형이 없는 의존성은 `release` 변형을 가져다 씁니다.

```
./gradlew :app:assembleStaging
./gradlew :app:bundleRelease
```

### 릴리스 서명

프로젝트 루트에 `keystore.properties` 파일이 있을 때만 릴리스 빌드에 서명합니다. 이 파일이 없으면 `assembleRelease`는 서명하지 않은 APK를 만들고, 다른 빌드는 그대로 진행합니다. git은 이 파일을 추적하지 않습니다.

```
KEYSTORE_PATH=/path/outside/the/repository/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=upload
KEY_PASSWORD=...
```

키스토어 파일은 저장소 밖에 두세요. 앱 서명 키는 Google Play가 보관하고, 여기서 쓰는 키는 업로드 키입니다.

### R8

`staging`과 `release`는 `optimization` 블록 설정에 따라 R8로 축소와 난독화를 거칩니다. keep 규칙은 `app/src/main/keepRules/`에 있습니다. 앱이 직접 넣은 규칙은 줄 번호를 남기는 것 하나입니다. 이 규칙 덕분에 `app/build/outputs/mapping/release/mapping.txt`로 스택 트레이스를 원래 이름으로 되살릴 수 있습니다. 라이브러리 규칙은 각 라이브러리에 들어 있습니다.

## 인터넷 권한 없음

Gil은 인터넷 없이 동작하고, 빌드가 이 조건을 검사합니다. 애플리케이션 모듈마다 `verify<Variant>NoInternetPermission` 작업이 생깁니다. 이 작업은 병합된 매니페스트를 읽어서 `android.permission.INTERNET`이 있으면 실패합니다. 권한을 앱이 넣었든 라이브러리가 넣었든 똑같습니다. `./gradlew check`는 이 작업을 전부 실행하고, `./gradlew verifyNoInternetPermission`은 이 작업만 실행합니다.

## 컨벤션 플러그인

공통 빌드 설정은 포함된 빌드(included build)인 `build-logic/convention`에 있습니다. 모듈 빌드 파일은 아래 플러그인을 한두 개 적용한 다음, 그 모듈의 의존성만 적습니다.

| 플러그인 | 적용하는 곳 | 더하는 것 |
|---|---|---|
| `gil.jvm.library` | Android 코드가 없는 모듈: `core:domain`, 기능 모듈의 `api`와 `impl:domain` | Kotlin JVM, 직렬화, 코루틴, 단위 테스트 |
| `gil.android.library` | Android 라이브러리 모듈 | 모듈 경로에서 만든 네임스페이스, SDK 수준, 단위 테스트와 기기 테스트 |
| `gil.android.library.compose` | 공유 UI 모듈(`core:designsystem`, `core:presentation`, `core:navigation`) | 위 설정과 Compose, Material 3 |
| `gil.android.feature` | `feature:<name>:impl:presentation` | 위 설정과 Hilt, ViewModel 지원, 공유 UI 모듈 |
| `gil.android.application` | 애플리케이션 모듈 | 카탈로그에 있는 애플리케이션 ID, 버전, SDK 수준과 인터넷 권한 검사 |
| `gil.android.application.compose` | `:app` | 위 설정과 Compose, Navigation 3 ViewModel 데코레이터 |
| `gil.hilt` | `@Inject` 클래스나 Hilt 모듈이 있는 모듈 | KSP로 처리하는 Hilt |
| `gil.room` | Room 데이터베이스를 가진 모듈 | KSP로 처리하는 Room. 스키마는 `schemas/`에 내보냄 |
| `gil.root` | 루트 프로젝트 | `check` 작업과 문서 번역 짝 검사 |

플러그인은 `build-logic/convention/build.gradle.kts`에 카탈로그의 ID로 등록하고, 모듈은 별칭으로 적용합니다.

```kotlin
plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}
```

## 테스트

단위 테스트는 JUnit 6(Jupiter)에서 AssertK, Turbine, `kotlinx-coroutines-test`와 함께 돌아갑니다. 이 의존성은 위 플러그인이 모든 모듈에 넣습니다.

```
./gradlew :<module>:testDebugUnitTest   # Android module
./gradlew :<module>:test                # JVM module
```

기기 테스트는 `src/androidTest`에 둡니다. 이 디렉터리가 없는 모듈은 테스트 APK를 만들지 않고, 기기 테스트 의존성도 받지 않습니다.

## 검사

PR을 올리기 전에 CI와 같은 명령을 돌려 보세요.

```
./gradlew spotlessCheck verifyDocTranslations lintDebug testDebugUnitTest assembleDebug verifyNoInternetPermission
```

### 코드 서식

Spotless가 모든 `.kt`와 `.kts` 파일을 ktlint `android_studio` 스타일과 compose-rules의 Compose 규칙으로 정리합니다. 파일 맨 위에는 `spotless/license-header.txt`의 Apache 2.0 라이선스 헤더를 붙입니다. 규칙 설정은 `.editorconfig`에 있습니다.

- Material 2 API는 쓸 수 없습니다.
- 컴포저블의 컬렉션 파라미터는 안정(stable) 타입이어야 합니다.
- 프리뷰 함수 이름은 `Preview`로 끝납니다.
- `CompositionLocal`은 디자인 시스템 토큰인 `LocalSpacing`과 `LocalMotion`만 만들 수 있습니다.

```
./gradlew spotlessApply   # fix
./gradlew spotlessCheck   # verify
```

### Lint

Android Lint는 경고가 하나라도 있으면 빌드를 실패시킵니다. 글자와 접근성을 지키는 검사는 오류로 올렸습니다: `HardcodedText`, `ContentDescription`, `SmallSp`, `SpUsage`, `TypographyEllipsis`, `TypographyDashes`, `UnusedResources`, `MissingTranslation`. 의존성 새 버전을 알려 주기만 하는 검사는 껐습니다. 버전 업데이트는 Dependabot이 맡습니다. 앱 모듈은 `checkDependencies`를 켜고 돌기 때문에, 앱이 쓰는 모든 모듈에서 쓰지 않는 리소스와 빠진 번역을 찾아냅니다.

일부러 남겨 둘 경고가 있다면 가장 좁은 범위에서 끄세요. 요소 하나에 `tools:ignore`를 달거나 선언 하나에 `@Suppress`를 다는 식입니다. 왜 끄는지는 주석으로 남기세요.

### 문서 번역 짝

공개 문서는 모두 영어(`name.md`)와 한국어(`name.ko.md`) 두 벌이고, 두 파일은 맨 위 다섯 줄 안에서 서로를 링크합니다. 이슈 양식도 같은 방식으로 짝을 이룹니다(`name.yml`, `name.ko.yml`). PR 템플릿은 한 파일에 두 언어를 함께 씁니다. `LICENSE`와 `NOTICE`는 법률 원문이라 영어로만 둡니다.

`./gradlew verifyDocTranslations`가 짝과 링크를 검사합니다. git이 무시하지 않는 파일만 보고, `./gradlew check`에도 들어 있습니다.

### CI

`main`에 push하거나 PR을 열 때마다 `.github/workflows/ci.yml`이 위 명령을 실행합니다. 워크플로는 저장소를 읽기만 할 수 있고, 액션은 모두 커밋 SHA로 고정했습니다. Dependabot(`.github/dependabot.yml`)은 일주일에 한 번 Gradle과 GitHub Actions 업데이트를 제안합니다. Kotlin과 KSP는 함께 바뀌어야 하므로 PR 하나로 묶어서 옵니다.

## 설계 결정

- **`kotlin-android` 플러그인을 쓰지 않습니다.** AGP 9가 Kotlin을 직접 컴파일하기 때문에, 그 위에 Kotlin Android 플러그인을 적용하면 빌드가 실패합니다.
- **플러그인 ID는 역도메인이 아니라 `gil.`로 시작합니다.** 플러그인을 외부에 배포하지 않으므로 이 빌드 안에서만 겹치지 않으면 됩니다.
- **카탈로그의 컨벤션 플러그인 항목에는 버전이 없습니다.** 루트 프로젝트가 `gil.root`를 적용하면 `build-logic`이 모든 모듈의 클래스패스에 올라갑니다. 이때 같은 플러그인을 버전과 함께 요청하면 Gradle이 거부합니다. `unspecified`라고 적어도 마찬가지입니다.
- **Spotless 설정은 루트에 하나만 둡니다.** 대상 목록 하나로 모든 모듈과 `build-logic`을 다루고, `.gitattributes`에 맞춰 줄바꿈을 LF로 씁니다.
