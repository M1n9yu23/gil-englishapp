# UI

[English](ui.md)

화면은 `core:designsystem`으로 만듭니다. 컴포넌트를 기기에서 보려면 카탈로그 앱을 설치하세요.

```
./gradlew :catalog:installDebug
```

## 테마

내용은 `GilTheme`으로 감쌉니다. 색은 `MaterialTheme.colorScheme`, 글자 모양은 `MaterialTheme.typography`에서 가져오고, 기능 코드에서 색을 따로 정하지 않습니다.
다이내믹 컬러는 쓰지 않으므로 어느 폰에서나 색이 같습니다.

간격과 애니메이션 시간은 `GilTheme.spacing`과 `GilTheme.motion`에서 가져옵니다.
한 파일에서만 쓰는 크기는 그 파일 위쪽에 이름 붙인 private 값으로 둡니다. `PathScreen.kt`의 `PathTokens`처럼 object 하나에 모으는 경우가 많습니다.

## 컴포넌트

직접 만들기 전에 그 역할의 디자인 시스템 컴포넌트가 있는지 봅니다. 화면의 주된 동작 하나에는 `PrimaryActionButton`, 목록의 줄에는 `ListRow`, 아직 내용이 없는 화면에는 `LoadingState`, `EmptyState`, `ErrorState`를 씁니다.
언제 무엇을 쓰는지는 각 컴포넌트의 KDoc에 있습니다.
누를 수 있는 레이아웃인데 맞는 컴포넌트가 없다면 `PressableSurface`에서 시작하세요. 터치 영역, 물결 효과, 버튼 시맨틱스가 들어 있습니다.

아이콘은 `GilIcons`에서 가져옵니다. Material Symbols를 `core:designsystem`의 벡터 드로어블로 옮겨 둔 것입니다. 새 아이콘은 Material Symbols의 path 데이터를 그대로 복사해 그 폴더에 드로어블로 더하고, `GilIcons`에 속성을 하나 추가합니다.

## 영어 문장

학습자가 읽는 영어 문장은 모두 `EnglishText`로 그립니다. 영어를 세리프체로 보여 주고 영어로 표시해서, 화면 낭독기가 영어 음성으로 읽습니다.
한국어 문장 속 영어는 `appendEnglish`를, 영어를 백틱으로 감싼 한국어 설명은 `KoreanProse`를 씁니다.

## 문자열

사용자에게 보이는 문자열은 그 문자열을 쓰는 모듈의 `values/strings.xml`(영어)과 `values-ko/strings.xml`(한국어)에 모두 넣습니다.
이름은 기능 이름으로 시작합니다(`lesson_play_slowly`). 여러 기능이 같이 쓰는 문자열은 `common_`으로 시작하고 `core:designsystem`이나 `core:presentation`에 둡니다.
자리표시자는 `<xliff:g>`로 감싸고 `example`을 붙입니다. 한국어 복수형은 `other` 하나만 씁니다.

## 접근성

- 아이콘 버튼에는 동작을 말하는 `contentDescription`을 붙입니다. 글자 옆에 붙은 장식 아이콘은 `contentDescription = null`로 둡니다.
- 직접 만든 클릭 동작에는 `clickLabel`("단원 열기")을 붙여서, 화면 낭독기가 누르면 무슨 일이 생기는지 읽게 합니다.
- 가장 큰 글꼴 크기에서도 글자가 잘리지 않아야 합니다. 화면의 스크린샷 테스트 클래스에는 모두 `fontScale = 2f` 경우가 하나 이상 있습니다.
