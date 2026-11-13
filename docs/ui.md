# UI

[한국어](ui.ko.md)

Screens are built from `core:designsystem`. To see its components on a device, install the catalog app:

```
./gradlew :catalog:installDebug
```

## Theme

Wrap content in `GilTheme`. Colors come from `MaterialTheme.colorScheme` and text styles from `MaterialTheme.typography`; feature code does not define colors of its own.
The app does not use dynamic color, so the palette is the same on every phone.

Spacing and animation durations come from `GilTheme.spacing` and `GilTheme.motion`.
A size used in only one file goes in a private object at the top of that file, such as `PathTokens` in `PathScreen.kt`.

## Components

Use the design system component for a role before you build your own: `PrimaryActionButton` for the one main action of a screen, `ListRow` for rows in a list, `LoadingState`, `EmptyState` and `ErrorState` for a screen with no content yet.
Their KDoc says when to use each one.
For a pressable layout that no component covers, start from `PressableSurface`, which brings the touch target, ripple and button semantics.

Icons come from `GilIcons`, which holds Material Symbols as vector drawables in `core:designsystem`. To add one, copy the symbol's path data from Material Symbols into a new drawable there.

## English text

Every English sentence the learner reads goes through `EnglishText`. It sets English in a serif and marks it as English, so screen readers pronounce it with an English voice.
English inside a Korean sentence uses `appendEnglish`, and Korean content text with English in backticks uses `KoreanProse`.

## Strings

Every user-facing string goes in `values/strings.xml` (English) and `values-ko/strings.xml` (Korean) of the module that uses it.
Names start with the feature, as in `lesson_play_slowly`. Strings that several features share start with `common_` and live in `core:designsystem` or `core:presentation`.
Mark placeholders with `<xliff:g>` and give an `example`. Korean plurals use only `other`.

## Accessibility

- An icon button needs a `contentDescription` that names the action. A decorative icon next to a text label gets `contentDescription = null`.
- Name a custom click action with `clickLabel` ("open unit") so screen readers say what happens.
- Text must still fit at the largest font size. Each screen's screenshot test has a case with `fontScale = 2f`.
