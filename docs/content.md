# Learning content

[한국어](content.ko.md)

Everything the app teaches lives in `content/` as YAML. The build turns it into the database the app reads, and stops on any error.

```
./gradlew checkContent     # checks content/ and writes tools/content-compiler/build/reports/content/report.md
./gradlew lockContentIds   # run before committing new content
```

The report lists every lesson in course order with the items it introduces. Read it before writing a new unit to see what the learner already knows.

## Files

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

## IDs

IDs look like `word.run`, `sense.run.v.move-fast`, `lesson.a1-03.2`. Once an ID is in `ids.lock`, never change or reuse it: learners' progress points at it. To remove an item, delete it and mark its line in `ids.lock` as `sense.run.v.move-fast retired`.

## Words

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

Pronunciation comes from CMUdict. When CMUdict has several, or none, set `ipa: rʌn`.

## Lessons

A lesson lists what it teaches in `introduces` and then its steps. Each step has a `type`.

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

| `type` | Fields |
|---|---|
| `explain` | `text` (Korean, paragraphs split by a blank line), `examples` |
| `listen`, `shadow`, `dictation` | `en`, `ko` |
| `chooseMeaning` | `en`, `options` in Korean (3 or 4) |
| `listenChoose` | `en` (played, not shown), `options` |
| `chooseEnglish` | `ko`, `options` in English |
| `arrange` | `ko`, `en`, optional `accept` (other right orders), `extra` (up to 2 tiles) |
| `fillBlank` | `en` with one `___`, `ko`, and either `answers` (typed) or `options` |
| `spell` | `sense` |
| `minimalPair` | `words` (two), `answer`, `why` |
| `readPassage` | `sentences` (`en`, `ko`), `questions` (`ko`, `options`) |
| `dialogue` | `lines` (`speaker`, `en`, `ko`), optional `question`, `options` in English |

Options have `en` or `ko`. Mark one `correct: true` and give every other option a `why`. Steps that grade an item name it in `item`. In Korean text, put English in backticks.

## Sentences

English sentences are plain text with three optional marks:

- `{runs|sense.run.v.manage}` says which sense a word has, when the learner knows more than one.
- `[every morning]{when}` marks a chunk for the sentence breakdown. Roles: `who`, `do`, `what`, `where`, `when`.
- `@Mina` marks a name, which the vocabulary check skips.

YAML needs double quotes around a sentence that starts with `@`, `[` or `{`.

Add `literal` (a word-by-word Korean gloss) and `grammar` (a grammar note ID) to sentences that have a breakdown.

## What fails the check

- A sentence uses a word the learner has not met by that lesson. Lessons count in course order; a step can set `checkVocab: false` for letter and sound drills.
- An ID is malformed, duplicated, missing, or gone from `ids.lock`.
- A sense has no example, or a choice step has no single correct option or a wrong option without `why`.
- A choice is longer than 40 characters, a tile than 20, an explanation paragraph than 200.
- A character the app's fonts cannot draw, such as an emoji.
- A word has no pronunciation.

Warnings, such as a word with two senses taught and no mark, go to the report.
