# UI

[한국어](ui.ko.md)

Screens are built from `core:designsystem`. To see its components on a device, install the catalog app:

```
./gradlew :catalog:installDebug
```

## Theme

Wrap content in `GilTheme`. Colors come from `MaterialTheme.colorScheme` and text styles from `MaterialTheme.typography`; feature screens do not define colors of their own.
The home screen widget is drawn by Glance outside the Compose theme, so it reads the same `LightColorScheme` and `DarkColorScheme` directly.
The app does not use dynamic color, so the palette is the same on every phone. `ColorContrastTest` checks the contrast of text and icon colors on the backgrounds in light and dark, so run it when you change `Color.kt`.

Spacing and animation durations come from `GilTheme.spacing` and `GilTheme.motion`.
A size used in only one file is a named private value at the top of that file, often grouped in an object such as `PathTokens` in `PathScreen.kt`.

## Look

The app looks like a printed textbook: paper-colored backgrounds, ink-colored text, and the text itself as the main content.

- Lists are rows separated by dividers (`ListRow`), never cards. Nothing has a shadow.
- Errors and wrong answers use the ochre `error` color, not red, and `ErrorState` has no alarm icon.
- Serif type is only for the English the learner studies. Titles, labels and numbers use the UI font.
- A state is never shown by color alone. A wrong answer, for example, also says "Your choice" and "Right answer" (`AnswerOption`).
- Animations use the durations in `GilTheme.motion` and no bounce or overshoot.
- Messages appear in a snackbar, never a toast.
- A screen that is loading shows still placeholder blocks in the shape of its content, not a spinner: `LoadingState`, or the screen's own placeholder when its layout differs, such as `PathLoading` in `PathScreen.kt`.
- The app has no illustrations.

## Components

Use the design system component for a role before you build your own: `PrimaryActionButton` for the one main action of a screen, `ListRow` for rows in a list, `LoadingState`, `EmptyState` and `ErrorState` for a screen with no content yet.
Their KDoc says when to use each one.
For a pressable layout that no component covers, start from `PressableSurface`, which brings the touch target, ripple and button semantics.

Icons come from `GilIcons`, which holds Material Symbols Outlined as vector drawables in `core:designsystem`; the `Filled` version marks a selected state and draws the media controls. To add one, copy the symbol's path data from Material Symbols into a new drawable there and add a property for it to `GilIcons`.

## English text

Every English sentence the learner reads in a Compose screen goes through `EnglishText`. It sets English in a serif and marks it as English, so screen readers pronounce it with an English voice. The home screen widget is drawn with Glance and sets its sentence in the device serif.
English inside a Korean sentence uses `appendEnglish`, and Korean content text with English in backticks uses `KoreanProse`.

## Strings

Every user-facing string goes in `values/strings.xml` (English) and `values-ko/strings.xml` (Korean) of the module that uses it.
Names start with the feature, as in `lesson_play_slowly`. A design system component's own strings start with the component, as in `answer_option_picked`. Other strings that several features share start with `common_` and live in `core:designsystem` or `core:presentation`.
Mark placeholders with `<xliff:g>` and give an `example`. Korean plurals use only `other`.

## Accessibility

- An icon button needs a `contentDescription` that names the action. A decorative icon next to a text label gets `contentDescription = null`.
- Name a custom click action with `clickLabel` ("open unit") so screen readers say what happens.
- Text must still fit at the largest font size. Every screenshot test class for a screen has at least one case at `fontScale = 2f`.
