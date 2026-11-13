# 기여 안내

[English](CONTRIBUTING.md)

버그 신고, 학습 내용 고치기, 코드 변경 모두 환영합니다.
참여하는 모든 사람은 [행동 강령](docs/translations/CODE_OF_CONDUCT.ko.md)을 따릅니다.

## 이슈

이슈 양식은 영어판과 한국어판이 있습니다. 편한 쪽을 고르세요.

| 양식 | 이럴 때 쓰세요 |
|---|---|
| 버그 신고 | 앱이 멈추거나 잘못 동작할 때 |
| 학습 내용 오류 신고 | 문장, 번역, 설명, 발음이 틀렸거나 알아보기 어려울 때 |
| 기능 제안 | 공부할 때 더 나았으면 하는 점이 있을 때 |

앱의 단어 화면과 문장 화면에서 '내용 오류 신고'를 누르면 학습 내용 오류 신고 양식이 열립니다. 위치와 앱 버전은 미리 채워져 있습니다.

Gil은 인터넷 없이 동작하고 계정, 광고, 분석 도구가 없습니다. 서버나 인터넷 연결이 필요한 기능은 받지 않습니다.

보안 문제는 이슈로 올리지 말고 [보안 정책](SECURITY.ko.md)에 적힌 방법으로 알려 주세요.

## 풀 리퀘스트

변경이 크다면 코드를 쓰기 전에 이슈를 열어 방향을 먼저 정하세요.
풀 리퀘스트 하나에는 변경 하나만 담고, 열기 전에 [빌드 문서](docs/build.ko.md#풀-리퀘스트-전에)의 검사를 돌리세요.

아래 규칙은 빌드가 검사하지 않습니다.

- 분석, 크래시 보고, 광고처럼 데이터를 휴대폰 밖으로 보내는 라이브러리는 넣지 마세요.
- 새 라이브러리나 폰트 같은 번들 파일은 Apache 2.0, MIT, BSD, OFL 중 하나여야 합니다. `NOTICE`에 추가하세요. Gradle 의존성이 아니라면 앱 라이선스 화면에 보이도록 `app/aboutlibraries/`에도 추가하세요.
- 공개 문서는 영어 `name.md`와 한국어 `name.ko.md` 짝입니다. 같은 풀 리퀘스트에서 둘 다 고치세요.

## 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/) 형식으로 영어로 씁니다. 범위에는 변경한 기능 모듈이나 core 모듈 이름을 씁니다. `lesson`, `designsystem` 같은 식입니다.

```
feat(review): add a test to skip a stage the learner already knows
fix(settings): keep paragraph breaks in the Korean privacy policy
docs: say what the home screen widget shows in the privacy policy
```

## 학습 내용

학습 내용은 `content/`의 YAML입니다. 형식과 검사 방법은 [학습 내용 문서](docs/content.ko.md)에 있습니다.

문장과 설명은 직접 쓰세요. 교재, 기출문제, 사전, 다른 앱에서 가져오면 안 됩니다.
학습 내용은 Apache 2.0을 따르므로 CC BY-SA처럼 동일조건변경허락 라이선스인 단어 목록이나 데이터는 쓸 수 없습니다. 라이선스가 없는 목록도 마찬가지입니다.

## 라이선스

기여한 내용은 [Apache License 2.0](LICENSE)을 따르는 데 동의한 것으로 봅니다.
