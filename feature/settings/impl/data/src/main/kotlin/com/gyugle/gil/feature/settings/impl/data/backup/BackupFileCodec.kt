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
package com.gyugle.gil.feature.settings.impl.data.backup

import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.impl.data.repository.accentOf
import com.gyugle.gil.feature.settings.impl.data.repository.key
import com.gyugle.gil.feature.settings.impl.data.repository.speedOf
import com.gyugle.gil.feature.settings.impl.data.repository.themeOf
import kotlin.time.Instant
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** Everything a backup file restores. [progressSection] is the progress feature's own JSON. */
internal data class BackupContents(
    val progressSection: String,
    val goal: DailyGoal,
    val reminders: List<ReminderTime>,
    val settings: AppSettings
)

internal data class BackupFile(val createdAt: Instant, val appVersionCode: Long, val contents: BackupContents)

/**
 * Writes and reads [BackupFileDto]. A file comes from outside the app, so reading is strict: an
 * unknown field or section, a value out of range or a duplicate time rejects the whole file. The
 * progress section is only checked to be a JSON object here; the progress feature checks the rest.
 */
internal object BackupFileCodec {
    private val json = Json

    fun encode(file: BackupFile): String {
        val contents = file.contents
        val sections = mapOf(
            BackupFileDto.PROGRESS to json.parseToJsonElement(contents.progressSection).jsonObject,
            BackupFileDto.GOAL to json.encodeToJsonElement(
                GoalSectionDto.serializer(),
                GoalSectionDto(SECTION_VERSION, contents.goal.minutes)
            ).jsonObject,
            BackupFileDto.REMINDERS to json.encodeToJsonElement(
                RemindersSectionDto.serializer(),
                RemindersSectionDto(SECTION_VERSION, contents.reminders.map { it.text() })
            ).jsonObject,
            BackupFileDto.SETTINGS to json.encodeToJsonElement(
                SettingsSectionDto.serializer(),
                contents.settings.toDto()
            ).jsonObject
        )
        val dto = BackupFileDto(
            format = BackupFileDto.FORMAT,
            formatVersion = BackupFileDto.FORMAT_VERSION,
            appVersionCode = file.appVersionCode,
            createdAt = file.createdAt.toString(),
            sections = sections
        )
        return json.encodeToString(BackupFileDto.serializer(), dto)
    }

    /** Reads versions before anything else, so a newer file is reported as newer rather than broken. */
    fun decode(text: String): Outcome<BackupFile, BackupError.Format> {
        val root = parseObject(text) ?: return malformed()
        if (root["format"] != JsonPrimitive(BackupFileDto.FORMAT)) return malformed()
        versionError(root["formatVersion"], BackupFileDto.FORMAT_VERSION)?.let { return Outcome.Failure(it) }
        val dto = decodeOrNull(BackupFileDto.serializer(), root) ?: return malformed()
        if (dto.sections.keys != BackupFileDto.SECTIONS) return malformed()
        val sections = dto.sections
        listOf(BackupFileDto.GOAL, BackupFileDto.REMINDERS, BackupFileDto.SETTINGS).forEach { name ->
            versionError(sections.getValue(name)["version"], SECTION_VERSION)?.let { return Outcome.Failure(it) }
        }
        val createdAt = parseInstantOrNull(dto.createdAt) ?: return malformed()
        val goal = decodeOrNull(GoalSectionDto.serializer(), sections.getValue(BackupFileDto.GOAL))
            ?.let { DailyGoal.ofMinutes(it.minutes) }
            ?: return malformed()
        val reminders = decodeOrNull(RemindersSectionDto.serializer(), sections.getValue(BackupFileDto.REMINDERS))
            ?.let { reminderTimesOrNull(it.times) }
            ?: return malformed()
        val settings = decodeOrNull(SettingsSectionDto.serializer(), sections.getValue(BackupFileDto.SETTINGS))
            ?.toSettingsOrNull()
            ?: return malformed()
        val progress = json.encodeToString(JsonObject.serializer(), sections.getValue(BackupFileDto.PROGRESS))
        return Outcome.Success(
            BackupFile(createdAt, dto.appVersionCode, BackupContents(progress, goal, reminders, settings))
        )
    }

    private fun malformed() = Outcome.Failure(BackupError.Format.MALFORMED)

    private fun versionError(element: Any?, supported: Int): BackupError.Format? {
        val version = (element as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        return when {
            version == null || version < 1 -> BackupError.Format.MALFORMED
            version > supported -> BackupError.Format.NEWER_VERSION
            else -> null
        }
    }

    // SerializationException and the date parse errors are IllegalArgumentExceptions.
    private fun parseObject(text: String): JsonObject? = try {
        json.parseToJsonElement(text) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun <T> decodeOrNull(deserializer: DeserializationStrategy<T>, element: JsonObject): T? = try {
        json.decodeFromJsonElement(deserializer, element)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun parseInstantOrNull(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun reminderTimesOrNull(texts: List<String>): List<ReminderTime>? {
        if (texts.size > ReminderTime.MAX_PER_DAY) return null
        val times = texts.map { text ->
            val match = TIME.matchEntire(text) ?: return null
            ReminderTime(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        }
        return times.takeIf { it.distinct().size == it.size }?.sorted()
    }

    // Not String.format, which would write the digits of the phone's locale.
    private fun ReminderTime.text(): String = "${hour.twoDigits()}:${minute.twoDigits()}"

    private fun Int.twoDigits(): String = toString().padStart(2, '0')

    private fun AppSettings.toDto() = SettingsSectionDto(
        version = SECTION_VERSION,
        theme = theme.key,
        accent = accent.key,
        speed = speed.key,
        showHangulAid = showHangulAid
    )

    private fun SettingsSectionDto.toSettingsOrNull(): AppSettings? = AppSettings(
        theme = themeOf(theme) ?: return null,
        accent = accentOf(accent) ?: return null,
        speed = speedOf(speed) ?: return null,
        showHangulAid = showHangulAid
    )

    private val TIME = Regex("([01][0-9]|2[0-3]):([0-5][0-9])")
}
