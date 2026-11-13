/*
 * Copyright 2026 MinGyu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gyugle.gil.feature.review.impl.domain

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsNone
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SentenceBreakdown
import com.gyugle.gil.feature.curriculum.api.SentenceWord
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.review.impl.domain.model.ReviewCard
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ReviewDeckTest {
    private val stage = StageTag(StageId("stage.s1"), 1, LocalizedText("소리와 글자", "Sounds and letters"))

    private val three = SenseId("sense.three.num.count")
    private val two = SenseId("sense.two.num.count")
    private val four = SenseId("sense.four.num.count")
    private val hi = SenseId("sense.hi.interj.greeting")
    private val hello = SenseId("sense.hello.interj.greeting")
    private val thanks = PatternId("pattern.thank-you")
    private val plural = PatternId("pattern.plural-s")
    private val letterA = SoundId("sound.letter-a")
    private val f = SoundId("sound.f")
    private val v = SoundId("sound.v")
    private val p = SoundId("sound.p")
    private val blendL = SoundId("sound.blend-l")
    private val linking = SoundId("sound.linking")
    private val thVoiced = SoundId("sound.th-voiced")
    private val thVoiceless = SoundId("sound.th-voiceless")

    private val lexicon = FakeLexiconObserver(
        word(
            "three",
            three,
            "3, 셋",
            Sentence(1, "Two, three, four.", "둘, 셋, 넷."),
            Sentence(2, "Three.", "셋."),
            Sentence(3, "Three, two, three.", "셋, 둘, 셋.")
        ),
        word("two", two, "2, 둘"),
        word("four", four, "4, 넷"),
        word("hi", hi, "안녕"),
        word("hello", hello, "안녕"),
        PatternEntry(thanks, "Thank you.", "고마워요", null, stage, emptyList()),
        PatternEntry(plural, "~s", "여럿", null, stage, emptyList()),
        SoundEntry(letterA, "eɪ", "A a", "이름은 에이", stage, listOf(SoundExample("apple", "ˈæpəl"))),
        SoundEntry(f, "f", "f, ph", "윗니를 아랫입술에", stage, listOf(SoundExample("fan", "fæn"))),
        SoundEntry(v, "v", "v", "목을 울려요", stage, listOf(SoundExample("five", "faɪv"), SoundExample("van", "væn"))),
        SoundEntry(p, "p", "p", "두 입술", stage, listOf(SoundExample("pen", "pɛn"))),
        SoundEntry(
            blendL,
            "bl",
            "bl, cl, fl, gl, pl, sl",
            "두 자음을 이어서",
            stage,
            listOf(SoundExample("black", "blæk"), SoundExample("play", "pleɪ"), SoundExample("flag", "flæg"))
        ),
        SoundEntry(
            linking,
            "‿",
            "",
            "이어 읽기",
            stage,
            listOf(SoundExample("stand up", "stænd ʌp"), SoundExample("wake up", "weɪk ʌp"))
        ),
        SoundEntry(thVoiced, "ð", "th", "목을 울려요", stage, listOf(SoundExample("this", "ðɪs"))),
        SoundEntry(thVoiceless, "θ", "th", "바람만", stage, listOf(SoundExample("think", "θɪŋk")))
    ).apply {
        sentences += SentenceBreakdown(
            sentence = Sentence(1, "Two, three, four.", "둘, 셋, 넷."),
            words = listOf(
                SentenceWord(0, 3, WordId("word.two"), "two", meaning(two, "2, 둘")),
                SentenceWord(5, 10, WordId("word.three"), "three", meaning(three, "3, 셋")),
                SentenceWord(12, 16, WordId("word.four"), "four", meaning(four, "4, 넷"))
            ),
            chunks = emptyList(),
            literalKo = null,
            grammar = null
        )
        sentences += SentenceBreakdown(
            sentence = Sentence(2, "Three.", "셋."),
            words = listOf(SentenceWord(0, 5, WordId("word.three"), "three", meaning(three, "3, 셋"))),
            chunks = emptyList(),
            literalKo = null,
            grammar = null
        )
        // The sense twice, apart: a blank would swallow the word between.
        sentences += SentenceBreakdown(
            sentence = Sentence(3, "Three, two, three.", "셋, 둘, 셋."),
            words = listOf(
                SentenceWord(0, 5, WordId("word.three"), "three", meaning(three, "3, 셋")),
                SentenceWord(12, 17, WordId("word.three"), "three", meaning(three, "3, 셋"))
            ),
            chunks = emptyList(),
            literalKo = null,
            grammar = null
        )
    }
    private val deck = ReviewDeck(lexicon)

    private fun word(text: String, id: SenseId, ko: String, vararg examples: Sentence) = WordEntry(
        id = WordId("word.$text"),
        text = text,
        ipa = text,
        hangul = null,
        senses = listOf(WordSense(id, PartOfSpeech.NUMBER, ko, null, null, stage, examples.toList())),
        collocations = emptyList(),
        family = emptyList(),
        notes = emptyList()
    )

    private fun meaning(id: SenseId, ko: String) = ItemMeaning(id, PartOfSpeech.NUMBER, ko, stage)

    private fun card(id: StudyItemId, stability: Double = 1.0, reviews: Int = 1) = CardState(
        id,
        stability,
        dueDay = StudyDay(10),
        lastReviewDay = StudyDay(5),
        reviewCount = reviews,
        lapseCount = 0
    )

    private suspend fun pool(vararg ids: StudyItemId): List<LessonItem> =
        (lexicon.getItems(ids.toList()) as Outcome.Success).data

    private suspend fun cardFor(card: CardState, pool: List<LessonItem>, hasVoice: Boolean = true, seed: Int = 1) =
        (deck.cardFor(card, pool, hasVoice, Random(seed)) as Outcome.Success).data

    @Test
    fun cardFor_newWord_choosesItsMeaningAmongOtherMeanings() = runTest {
        val made = cardFor(card(three), pool(three, two, four, thanks, letterA))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.ChooseMeaning>().given {
            assertThat(it.options[it.answer]).isEqualTo("3, 셋")
            // Words first, then a pattern; a sound has no meaning to offer.
            assertThat(it.options).containsExactlyInAnyOrder("3, 셋", "2, 둘", "4, 넷", "고마워요")
            assertThat(it.speakable).isEqualTo(true)
        }
    }

    @Test
    fun cardFor_chooseEnglish_leavesOutItemsWithTheSameMeaning() = runTest {
        val made = cardFor(card(hi, stability = 8.0, reviews = 3), pool(hi, hello, two))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.ChooseEnglish>().given {
            assertThat(it.options[it.answer]).isEqualTo("hi")
            assertThat(it.options).doesNotContain("hello")
        }
    }

    @Test
    fun cardFor_onlyItemsWithTheSameMeaning_fallsBackToSpelling() = runTest {
        val made = cardFor(card(hi, stability = 8.0, reviews = 3), pool(hi, hello), hasVoice = false)

        assertThat(made).isEqualTo(ReviewCard.Spell(hi, "hi", "안녕", "hi", speakable = false))
    }

    @Test
    fun cardFor_wellKnownWord_blanksItInASentenceWithOtherWords() = runTest {
        // Every seed must pass over the sentence that is only the word and the one with it twice.
        for (seed in 1..10) {
            val made = cardFor(card(three, stability = 30.0, reviews = 4), pool(three, two), seed = seed)

            assertThat(made).isEqualTo(
                ReviewCard.FillBlank(three, "Two, three, four.", 5, 10, "둘, 셋, 넷.", "3, 셋")
            )
        }
        val blank = cardFor(card(three, stability = 30.0, reviews = 4), pool(three, two)) as ReviewCard.FillBlank
        assertThat(blank.answer).isEqualTo("three")
    }

    @Test
    fun cardFor_wellKnownWordWithoutAUsableSentence_isSpelled() = runTest {
        val made = cardFor(card(four, stability = 30.0, reviews = 4), pool(four, two), hasVoice = false)

        assertThat(made).isEqualTo(ReviewCard.Spell(four, "four", "4, 넷", "four", speakable = false))
    }

    @Test
    fun cardFor_patternWithAPlaceholder_isNeverHeardOrSpelled() = runTest {
        val made = cardFor(card(plural, stability = 8.0, reviews = 2), pool(plural, thanks))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.ChooseEnglish>()
        val alone = cardFor(card(plural, stability = 30.0), pool(plural))
        assertThat(alone).isNull()
    }

    @Test
    fun cardFor_listening_offersOnlyWordsThatCanBeSaid() = runTest {
        val made = cardFor(card(thanks, stability = 8.0, reviews = 2), pool(thanks, plural, two))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.ListenChoose>().given {
            assertThat(it.spoken).isEqualTo("Thank you.")
            assertThat(it.options).containsExactlyInAnyOrder("Thank you.", "two")
        }
    }

    @Test
    fun cardFor_letter_saysTheCapitalAndFillsOptionsFromTheAlphabet() = runTest {
        val made = cardFor(card(letterA), pool(letterA), hasVoice = false)

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.ChooseLetter>().given {
            assertThat(it.spoken).isEqualTo("A")
            assertThat(it.nameIpa).isEqualTo("eɪ")
            assertThat(it.options[it.answer]).isEqualTo("A a")
            assertThat(it.options.size).isEqualTo(MAX_DISTRACTORS + 1)
            assertThat(it.options.distinct().size).isEqualTo(it.options.size)
        }
    }

    @Test
    fun cardFor_sound_wrongWordsDoNotHaveTheSound() = runTest {
        for (seed in 1..10) {
            val made = cardFor(card(f), pool(f, v, p, letterA), seed = seed)

            assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
                assertThat(it.options[it.answer].text).isEqualTo("fan")
                // five has an f sound, so only van can stand for v; letter words come last.
                assertThat(it.options.map { option -> option.text })
                    .containsExactlyInAnyOrder("fan", "van", "pen", "apple")
            }
        }
    }

    @Test
    fun cardFor_soundForAGroup_answersWithAnExampleThatHasTheShownSymbol() = runTest {
        for (seed in 1..10) {
            val made = cardFor(card(blendL), pool(blendL, f, v, p), seed = seed)

            assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
                // play and flag are l blends too, but not the bl the card shows.
                assertThat(it.options[it.answer].text).isEqualTo("black")
                assertThat(it.options.map { option -> option.text }).containsNone("play", "flag")
            }
        }
    }

    @Test
    fun cardFor_sound_namesItForScreenReadersBySpelling() = runTest {
        val made = cardFor(card(f), pool(f, v))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
            assertThat(it.spelling).isEqualTo("f, ph")
        }
    }

    @Test
    fun cardFor_soundSpelledLikeAnother_takesNoWrongOptionFromIt() = runTest {
        for (seed in 1..10) {
            val made = cardFor(card(thVoiced), pool(thVoiced, thVoiceless, p), seed = seed)

            assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
                // Read as "th", think would answer the card as well as this.
                assertThat(it.options.map { option -> option.text }).containsExactlyInAnyOrder("this", "pen")
            }
        }
    }

    @Test
    fun cardFor_symbolNoExampleSpells_answersWithAnyExample() = runTest {
        val made = cardFor(card(linking), pool(linking, f))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
            assertThat(it.options[it.answer].text).isIn("stand up", "wake up")
        }
    }

    @Test
    fun cardFor_soundLearnedAlone_takesWrongOptionsFromLetterWords() = runTest {
        val made = cardFor(card(f), pool(f, letterA))

        assertThat(made).isNotNull().isInstanceOf<ReviewCard.FindSound>().given {
            assertThat(it.options.map { option -> option.text }).containsExactlyInAnyOrder("fan", "apple")
        }
    }

    @Test
    fun cardFor_soundWithNoOtherSoundOrLetter_null() = runTest {
        assertThat(cardFor(card(f), pool(f))).isNull()
    }

    @Test
    fun cardFor_itemNoLongerInTheContent_null() = runTest {
        assertThat(cardFor(card(SenseId("sense.gone.n.away")), pool(three, two))).isNull()
    }

    @Test
    fun cardFor_dictionaryUnavailable_fails() = runTest {
        val items = pool(three, two)
        lexicon.error = ContentError.UNAVAILABLE

        assertThat(deck.cardFor(card(three, stability = 30.0, reviews = 4), items, true, Random(1)))
            .isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    @Test
    fun cardFor_sameSeed_sameCard() = runTest {
        val items = pool(three, two, four, thanks)

        assertThat(cardFor(card(three), items, seed = 7)).isEqualTo(cardFor(card(three), items, seed = 7))
    }
}
