# 빌드

[English](build.md)

```
./gradlew :app:installDebug     # 평소 개발용
./gradlew :app:installStaging   # R8을 거친 release 코드, debug 키로 서명, debug 빌드와 함께 설치됨
```

R8을 거친 뒤 동작이 달라질 수 있는 변경은 `staging`으로 확인하세요.

## 풀 리퀘스트 전에

```
./gradlew spotlessApply
./gradlew spotlessCheck lintDebug testDebugUnitTest verifyNoInternetPermission
```

CI는 풀 리퀘스트마다 같은 검사와 debug 빌드를 실행합니다.

## 빌드가 막는 것

- Lint 경고가 하나라도 있으면 빌드가 실패합니다. 일부러 남길 경고는 가장 좁은 범위에서 억제하고 이유를 주석으로 적으세요.
- 사용자에게 보이는 문자열은 영어(`values/`)와 한국어(`values-ko/`)가 모두 있어야 합니다.
- `.kt`, `.kts` 파일에는 라이선스 헤더가 있어야 합니다. `spotlessApply`가 넣어 줍니다.
- 앱은 INTERNET 권한을 요청하면 안 됩니다. 라이브러리가 넣은 권한도 마찬가지입니다.
- `content/`의 학습 내용은 검사를 통과해야 합니다. [content.ko.md](content.ko.md)를 보세요.
