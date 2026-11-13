# 학습 내용

[English](content.md)

앱이 가르치는 내용은 모두 `content/`에 YAML로 있습니다. 빌드할 때 이 파일로 앱이 읽는 데이터베이스를 만들고, 내용에 오류가 하나라도 있으면 빌드가 실패합니다.

```
./gradlew checkContent     # content/를 검사하고 tools/content-compiler/build/reports/content/report.md를 씀
./gradlew lockContentIds   # 새 ID를 content/ids.lock에 추가. 새 내용을 커밋하기 전에 실행
```

보고서 맨 위에는 오류와 경고가 나오고, 그 아래에 모든 수업이 코스 순서대로 나옵니다. 수업마다 새로 가르치는 항목과, 그 수업을 마친 학습자가 아는 항목 수가 붙어 있습니다. 단원을 새로 쓰기 전에 읽어 보세요.

## 파일

```
content/
  course.yaml           # 단계 순서, 단계별 단원, 굴절형을 쓸 수 있는 시점
  units/s1/s1-19.yaml   # 단원 하나와 그 수업들
  lexicon/*.yaml        # 단어, 뜻, 예문
  patterns.yaml         # "Thank you." 같은 표현 덩어리
  sounds.yaml           # 글자와 소리
  ids.lock              # 공개된 모든 ID
```

파일 확장자는 `.yaml`이어야 합니다. 모르는 키는 오류라서 필드 이름에 오타가 있으면 검사가 실패합니다.

## ID

| 항목 | 예 |
|---|---|
| 단계, 단원, 수업 | `stage.s1`, `unit.s1-19`, `lesson.s1-19.1` |
| 단어, 뜻 | `word.zero`, `sense.zero.num.count` |
| 표현, 소리 | `pattern.thank-you`, `sound.letter-a` |

단계 코드는 `s0`, `s1`, `a1`, `a2`, `b1`, `b2`, `c1`입니다. 단원 파일은 `units/<단계>/<단원>.yaml`에 두고, `course.yaml`의 해당 단계 아래에 올려야 합니다.

학습 기록이 ID를 가리키므로 `ids.lock`에 들어간 ID는 바꾸거나 다시 쓰면 안 됩니다. 잠긴 ID가 내용에서 사라지면 검사가 실패합니다. 항목을 없앨 때는 내용에서 지우고, `ids.lock`의 그 줄을 `sense.zero.num.count retired`처럼 바꾸세요.

## 단어

```yaml
- word: zero
  id: word.zero
  ipa: ˈzɪroʊ                 # CMUdict 대신 이 발음을 씀
  hangul: 지로우               # 한글 발음 보조
  senses:
    - id: sense.zero.num.count  # sense.<단어>.<품사>.<뜻>
      pos: number
      ko: 0, 영
      examples:
        - en: Zero, one, two.
          ko: 영, 일, 이.
  notes:
    - kind: usage             # konglish, mistake, usage 중 하나
      ko: "전화번호에서는 `zero` 대신 알파벳 이름처럼 `oh`라고 읽을 때가 많아요."
```

`pos`에는 품사를 풀어 쓰고(`noun`, `verb`, `number`), 뜻 ID에는 줄임말(`n`, `v`, `num`)을 씁니다. 뜻마다 예문이 하나 이상 있어야 합니다.

`forms`에는 불규칙형만 적습니다. 키는 `plural`, `third`, `past`, `pastParticiple`, `ing`, `comparative`, `superlative`입니다. 불규칙 `past`를 적었다면 `pastParticiple`도 함께 적으세요.

발음은 CMUdict에서 가져옵니다. CMUdict에 없는 단어는 `ipa`를 직접 적어야 합니다. 발음이 여러 개면 첫 번째를 쓰고 보고서에 경고가 나옵니다. 다른 발음을 쓰려면 `ipa`를 적으세요.

`patterns.yaml`의 항목에는 `id`, `text`, `ko`, 필요하면 `note`, 그리고 `examples`가 있습니다. `sounds.yaml`의 항목에는 `id`, `symbol`(IPA), `spelling`, `ko`, `examples`(단어 목록)가 있습니다.

## 수업

수업은 `introduces`에 새로 가르치는 뜻, 표현, 소리를 적고 그 아래에 단계를 나열합니다.

```yaml
- id: lesson.s1-19.1
  title: { ko: 만날 때와 헤어질 때, en: Hello and goodbye }
  goal: { ko: 만나고 헤어질 때 인사할 수 있어요, en: Say hello and goodbye }
  introduces: [sense.hello.interj.greeting, sense.hi.interj.greeting]
  steps:
    - type: listen
      en: "Hello, @Sam."
      ko: "안녕하세요, 샘 씨."
    - type: chooseMeaning
      en: Hi.
      item: sense.hi.interj.greeting
      options:
        - { ko: 안녕하세요(만날 때), correct: true }
        - { ko: 잘 가요(헤어질 때), why: "`hi`는 만날 때 하는 인사예요. 헤어질 때는 `bye`라고 해요." }
        - { ko: 높은, why: "소리는 `high`(높은)와 같아요. 인사로 쓰는 `hi`는 만날 때 하는 말이에요." }
```

| `type` | 필드 |
|---|---|
| `explain` | `text`(한국어, 빈 줄로 문단 구분), `examples` |
| `listen` | `en`, `ko`, 필요하면 `literal` |
| `shadow`, `dictation` | `en`, `ko` |
| `chooseMeaning` | `en`, 한국어 `options` 3~4개 |
| `listenChoose` | `en`, 영어나 한국어 `options` 2~4개 |
| `chooseEnglish` | `ko`, 영어 `options` 2~4개 |
| `arrange` | `ko`, `en`, 필요하면 `accept`(같은 타일로 만드는 다른 정답 순서), `extra`(방해 타일 2개까지) |
| `fillBlank` | `___` 하나가 든 `en`, `ko`, 그리고 `answers`(직접 입력)나 영어 `options` 중 하나 |
| `spell` | `sense` |
| `minimalPair` | `words`(두 개), `answer`, 필요하면 `why` |
| `readPassage` | `sentences`(`en`, `ko`), `questions`(`ko`, `options`) |
| `dialogue` | `lines`(`speaker`, `en`, `ko`), 필요하면 `question`, 영어 `options` |

보기에는 `en`과 `ko` 중 하나를 씁니다. 정답 하나에만 `correct: true`를 달고, 나머지 보기에는 모두 `why`로 틀린 이유를 적으세요.

`chooseMeaning`, `listenChoose`, `chooseEnglish`, `dialogue`, `arrange`, `fillBlank`, `dictation`, `minimalPair`는 채점할 항목을 `item`에 적을 수 있습니다. `spell`은 `sense`에 적은 뜻을 채점합니다. 이 항목은 그 수업이나 앞 수업에서 가르친 것이어야 합니다.

`checkVocab: false`를 단계에 달면 그 단계만 어휘 검사를 하지 않습니다. 알파벳 연습이나 첫 수업들에 씁니다. `spell`과 `minimalPair`에는 달 수 없습니다.

한국어 글 안의 영어는 백틱으로 감싸세요.

## 문장

영어 문장은 평문으로 쓰고, 필요하면 세 가지 표시를 붙입니다.

- `{runs|sense.run.v.manage}`: 학습자가 아는 뜻이 둘 이상일 때, 이 문장에서 쓴 뜻을 정합니다.
- `[every morning]{when}`: 문장 뜯어보기에 쓸 덩어리와 역할입니다. 역할은 `who`, `do`, `what`, `where`, `when`입니다.
- `@Mina`: 사람 이름입니다. 어휘 검사에서 빠집니다.

`@`, `[`, `{`나 백틱으로 시작하는 YAML 문자열은 따옴표로 감싸야 합니다.

예문과 `listen` 단계에는 단어별 직역인 `literal`을 붙일 수 있습니다.

## 검사에서 실패하는 경우

- 그 수업까지 배우지 않은 단어가 문장에 나올 때. 복수형, `-ing`, 과거형 같은 형태를 `course.yaml`의 `inflections`가 그 형태에 짝지은 표현을 가르치는 수업보다 먼저 쓸 때도 실패합니다.
- ID 모양이 틀렸거나, 겹치거나, 가리키는 대상이 없을 때. 잠긴 ID가 사라졌을 때.
- 같은 항목을 두 번 가르칠 때. 어느 수업도 가르치지 않는 항목에 `level`이 없을 때.
- 예문 없는 뜻이 있을 때. 고르기 단계에 정답이 딱 하나가 아니거나, 틀린 보기에 `why`가 없거나, 같은 보기가 두 번 나올 때.
- 보기가 40자, 타일이 20자, 설명 문단이 200자를 넘을 때.
- 앱 글꼴로 그릴 수 없는 글자(이모지 등)가 있을 때.
- 발음을 찾을 수 없는 단어가 있을 때.

한국어 글에서 백틱 없이 쓴 영어, 문장 부호로 끝나지 않는 영어 문장 같은 문체 경고는 보고서에만 나오고 검사는 통과합니다.
