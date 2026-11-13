# Learning content

[한국어](content.ko.md)

Everything the app teaches is YAML in `content/`. The build turns it into the database the app reads and fails on any content error.

```
./gradlew checkContent     # checks content/ and writes tools/content-compiler/build/reports/content/report.md
./gradlew lockContentIds   # adds new IDs to content/ids.lock; run it before committing new content
```

The report lists errors and warnings first, then every lesson in course order with the items it introduces and how many items the learner knows after it. Read it before writing a unit.

## Files

```
content/
  course.yaml           # stages in order, the units of each stage, inflection gates
  units/s1/s1-19.yaml   # one unit and its lessons
  lexicon/*.yaml        # words, their senses and examples
  patterns.yaml         # chunks such as "Thank you."
  sounds.yaml           # letters and sounds
  ids.lock              # every published ID
```

Files must end in `.yaml`. Unknown keys are errors, so a typo in a field name fails the check.

## IDs

| Item | Example |
|---|---|
| Stage, unit, lesson | `stage.s1`, `unit.s1-19`, `lesson.s1-19.1` |
| Word, sense | `word.zero`, `sense.zero.num.count` |
| Pattern, sound | `pattern.thank-you`, `sound.letter-a` |

Stage codes are `s0`, `s1`, `a1`, `a2`, `b1`, `b2` and `c1`. A unit lives in `units/<stage>/<unit>.yaml` and must be listed under its stage in `course.yaml`.

Learners' progress points at IDs, so an ID in `ids.lock` must never change or be reused. If a locked ID disappears from the content, the check fails. To remove an item, delete it and change its line in `ids.lock` to `sense.zero.num.count retired`.

## Words

```yaml
- word: zero
  id: word.zero
  ipa: ˈzɪroʊ                 # overrides CMUdict
  hangul: 지로우               # Hangul pronunciation aid
  senses:
    - id: sense.zero.num.count  # sense.<word>.<part of speech>.<meaning>
      pos: number
      ko: 0, 영
      examples:
        - en: Zero, one, two.
          ko: 영, 일, 이.
  notes:
    - kind: usage             # konglish, mistake or usage
      ko: "전화번호에서는 `zero` 대신 알파벳 이름처럼 `oh`라고 읽을 때가 많아요."
```

`pos` is written in full (`noun`, `verb`, `number`); the sense ID uses the short form (`n`, `v`, `num`). Every sense needs an example.

`forms` lists irregular forms only: `plural`, `third`, `past`, `pastParticiple`, `ing`, `comparative`, `superlative`. Give `pastParticiple` together with an irregular `past`.

Pronunciation comes from CMUdict. A word CMUdict does not have needs `ipa`. When CMUdict has several pronunciations, the first is used and the report warns; set `ipa` to pick another.

`patterns.yaml` entries have `id`, `text`, `ko`, an optional `note` and `examples`. `sounds.yaml` entries have `id`, `symbol` (IPA), `spelling`, `ko` and `examples`, a list of words.

## Lessons

A lesson lists what it teaches in `introduces` (senses, patterns or sounds) and then its steps.

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

| `type` | Fields |
|---|---|
| `explain` | `text` (Korean, paragraphs split by a blank line), `examples` |
| `listen` | `en`, `ko`, optional `literal` |
| `shadow`, `dictation` | `en`, `ko` |
| `chooseMeaning` | `en`, 3 or 4 `options` in Korean |
| `listenChoose` | `en`, 2 to 4 `options` in either language |
| `chooseEnglish` | `ko`, 2 to 4 `options` in English |
| `arrange` | `ko`, `en`, optional `accept` (other right orders with the same tiles), `extra` (up to 2 tiles) |
| `fillBlank` | `en` with one `___`, `ko`, and either `answers` (typed) or English `options` |
| `spell` | `sense` |
| `minimalPair` | `words` (two), `answer`, optional `why` |
| `readPassage` | `sentences` (`en`, `ko`), `questions` (`ko`, `options`) |
| `dialogue` | `lines` (`speaker`, `en`, `ko`), optional `question`, English `options` |

Each option has `en` or `ko`. Mark exactly one `correct: true` and give every other option a `why`.

`chooseMeaning`, `listenChoose`, `chooseEnglish`, `dialogue`, `arrange`, `fillBlank`, `dictation` and `minimalPair` can name the item they grade in `item`; `spell` grades its `sense`. The item must be taught in that lesson or earlier.

`checkVocab: false` turns off the vocabulary check for one step, for letter drills and the first lessons. `spell` and `minimalPair` do not take it.

In Korean text, put English in backticks.

## Sentences

English sentences are plain text with three optional marks:

- `{runs|sense.run.v.manage}` says which sense a word has, when the learner knows more than one.
- `[every morning]{when}` marks a chunk for the sentence breakdown. Roles: `who`, `do`, `what`, `where`, `when`.
- `@Mina` marks a name, which the vocabulary check skips.

Quote a YAML string that starts with `@`, `[`, `{` or a backtick.

Examples and `listen` steps can add `literal`, a word-by-word Korean gloss.

## What fails the check

- A sentence uses a word the learner has not met by that lesson, or a form such as a plural, `-ing` or past before the lesson that introduces the pattern `inflections` in `course.yaml` maps that form to.
- An ID is malformed, duplicated or points at nothing, or a locked ID is gone.
- An item is introduced twice, or no lesson introduces it and it has no `level`.
- A sense has no example, or a choice step has no single correct option, a wrong option without `why`, or the same option twice.
- A choice is longer than 40 characters, a tile than 20, an explanation paragraph than 200.
- A character the app's fonts cannot draw, such as an emoji.
- A word has no pronunciation.

Style problems, such as English without backticks in Korean text or an English sentence without final punctuation, are warnings: they appear in the report and the check still passes.
