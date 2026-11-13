# Contributing

[한국어](CONTRIBUTING.ko.md)

Bug reports, corrections to the learning content and code changes are all welcome.
Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Issues

Each issue form comes in English and Korean. Use either.

| Form | Use it when |
|---|---|
| Bug report | the app crashes or does something wrong |
| Learning content error | a sentence, translation, explanation or pronunciation is wrong or unclear |
| Feature request | something could work better for studying |

In the app, "Report a content error" on a word or sentence screen opens the content error form with the location and app version filled in.

Gil works offline and has no accounts, ads or analytics, so ideas that need a server or an internet connection are out of scope.

Report security problems as described in [SECURITY.md](SECURITY.md), not in an issue.

## Pull requests

Open an issue first for anything bigger than a small fix, so the approach is settled before you write the code.
Keep one change per pull request, and run the checks in [docs/build.md](docs/build.md#before-a-pull-request) before you open it.

How the code is laid out and the rules it follows: [architecture](docs/architecture.md), [code style](docs/code-style.md), [testing](docs/testing.md) and [UI](docs/ui.md).

The build does not check these:

- Do not add libraries that send data off the phone, such as analytics, crash reporting or ads.
- A new library, font or other bundled file must be under Apache 2.0, MIT, BSD or OFL. Add it to `NOTICE`. If it is not a Gradle dependency, also add it under `app/aboutlibraries/` so the app's license screen lists it.
- Public documents come in pairs, `name.md` in English and `name.ko.md` in Korean. Change both in the same pull request.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) in English. The scope is the feature or core module the change is in, such as `lesson` or `designsystem`.

```
feat(review): add a test to skip a stage the learner already knows
fix(settings): keep paragraph breaks in the Korean privacy policy
docs: say what the home screen widget shows in the privacy policy
```

## Learning content

The content is YAML in `content/`. [docs/content.md](docs/content.md) describes the format and how to check it.

Write sentences and explanations yourself. Do not copy them from textbooks, exam papers, dictionaries or other apps.
The content is licensed under Apache 2.0, so word lists and data under share-alike licenses such as CC BY-SA cannot be used, nor can lists that have no license.

## License

By contributing, you agree that your contribution is licensed under the [Apache License 2.0](LICENSE).
