# 학습 내용

[English](content.md)

앱이 가르치는 내용은 모두 `content/` 폴더의 YAML 파일에 있습니다. 빌드할 때 이 파일로 앱이 읽는 데이터베이스를 만들고, 오류가 하나라도 있으면 빌드가 멈춥니다.

```
./gradlew checkContent     # checks content/ and writes tools/content-compiler/build/reports/content/report.md
./gradlew lockContentIds   # run before committing new content
```

보고서에는 모든 과가 코스 순서대로 나오고, 과마다 새로 가르치는 항목이 붙어 있습니다. 새 단원을 쓰기 전에 보고서를 열어 학습자가 이미 아는 것을 확인하세요.

## 파일

```
content/
  course.yaml          stages in order, the units of each stage, and inflections:
                       the item that must be taught before past, plural or other forms appear
  units/a1/a1-03.yaml  one unit and its lessons
  lexicon/*.yaml       words, their senses and examples
  patterns.yaml        chunks such as "I'd like ~"
  sounds.yaml          sounds of the first stages
  grammar/*.yaml       grammar notes that sentences link to
  ids.lock             every published ID
```

## ID

ID는 `word.run`, `sense.run.v.move-fast`, `lesson.a1-03.2` 같은 모양입니다. `ids.lock`에 들어간 ID는 바꾸거나 다시 쓰면 안 됩니다. 학습자의 기록이 이 ID를 가리키기 때문입니다. 항목을 없애려면 내용에서 지우고, `ids.lock`의 그 줄을 `sense.run.v.move-fast retired`처럼 바꾸세요.

## 단어

```yaml
- word: run
  id: word.run
  forms: { past: ran, pastParticiple: run }   # irregular forms only
  senses:
    - id: sense.run.v.move-fast                # sense.<word>.<pos>.<meaning>
      pos: verb
      ko: 달리다
      examples:
        - en: I run every morning.
          ko: 저는 매일 아침 달려요.
    - id: sense.run.v.manage
      pos: verb
      ko: 운영하다
      level: b1                                # needed while no lesson introduces it
      examples:
        - en: She runs a small cafe.
          ko: 그녀는 작은 카페를 운영해요.
```

발음기호는 CMUdict에서 가져옵니다. CMUdict에 발음이 여러 개이거나 아예 없으면 `ipa: rʌn`처럼 직접 적으세요.

## 과

과는 `introduces`에 새로 가르치는 항목을 적고, 그다음에 단계를 나열합니다. 단계마다 `type`으로 종류를 정합니다.

```yaml
- id: lesson.a1-01.2
  title: { ko: 매일 아침 달려요, en: I run every morning }
  goal: { ko: 매일 하는 일을 말할 수 있어요, en: Say what you do every day }
  introduces: [sense.run.v.move-fast, sense.every.det.each, sense.morning.n.early-day]
  steps:
    - type: listen
      en: I run every morning.
      ko: 저는 매일 아침 달려요.
    - type: chooseEnglish
      ko: 아침
      item: sense.morning.n.early-day
      options:
        - { en: morning, correct: true }
        - { en: student, why: "`student`는 학생이에요." }
```

| `type` | 필드 |
|---|---|
| `explain` | `text`(한국어, 빈 줄로 문단 구분), `examples` |
| `listen`, `shadow`, `dictation` | `en`, `ko` |
| `chooseMeaning` | `en`, 한국어 `options`(3~4개) |
| `listenChoose` | `en`(소리만 들려줌), `options` |
| `chooseEnglish` | `ko`, 영어 `options` |
| `arrange` | `ko`, `en`, 필요하면 `accept`(정답으로 칠 다른 순서), `extra`(방해 타일 2개까지) |
| `fillBlank` | `___` 하나가 든 `en`, `ko`, 그리고 `answers`(직접 입력)나 `options` 중 하나 |
| `spell` | `sense` |
| `minimalPair` | `words`(두 개), `answer`, `why` |
| `readPassage` | `sentences`(`en`, `ko`), `questions`(`ko`, `options`) |
| `dialogue` | `lines`(`speaker`, `en`, `ko`), 필요하면 `question`, 영어 `options` |

보기에는 `en`이나 `ko` 중 하나를 씁니다. 정답 하나에 `correct: true`를 달고, 나머지 보기에는 모두 `why`로 틀린 이유를 적으세요. 항목을 채점하는 단계는 `item`에 그 항목을 적습니다. 한국어 글 안의 영어는 백틱으로 감쌉니다.

## 문장

영어 문장은 평문으로 쓰고, 필요할 때 세 가지 표시를 붙입니다.

- `{runs|sense.run.v.manage}`: 학습자가 아는 뜻이 둘 이상일 때 이 문장에서 쓰인 뜻을 정합니다.
- `[every morning]{when}`: 문장 뜯어보기에 쓸 덩어리와 역할입니다. 역할은 `who`, `do`, `what`, `where`, `when`입니다.
- `@Mina`: 사람 이름입니다. 어휘 검사에서 빠집니다.

`@`, `[`, `{`로 시작하는 문장은 YAML에서 큰따옴표로 감싸야 합니다.

문장 뜯어보기가 있는 문장에는 `literal`(단어별 직역)과 `grammar`(문법 메모 ID)를 함께 적습니다.

## 검사에서 막히는 것

- 그 과까지 배우지 않은 단어가 문장에 나옵니다. 과는 코스 순서로 셉니다. 알파벳이나 소리 연습 단계는 `checkVocab: false`로 검사를 끌 수 있습니다.
- ID 모양이 틀렸거나, 겹치거나, 가리키는 대상이 없거나, `ids.lock`에 있던 ID가 사라졌습니다.
- 예문 없는 뜻이 있습니다. 고르기 단계에 정답이 딱 하나가 아니거나, 틀린 보기에 `why`가 없습니다.
- 보기가 40자, 타일이 20자, 설명 문단이 200자를 넘습니다.
- 앱 글꼴로 그릴 수 없는 글자(이모지 등)가 있습니다.
- 발음기호를 찾을 수 없는 단어가 있습니다.

배운 뜻이 둘인데 표시가 없는 단어 같은 경고는 보고서에만 나옵니다.
