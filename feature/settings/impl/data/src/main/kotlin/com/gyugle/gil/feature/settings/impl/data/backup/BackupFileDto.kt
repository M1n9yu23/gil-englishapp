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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A backup file, format version 1. Field names and the section names are the file format:
 * renaming one breaks files people already saved. A change older files cannot satisfy needs a new
 * [formatVersion], or a new version inside the section it touches, and a reader for the old one.
 *
 * Each section carries its own `version`. The progress section belongs to the progress feature,
 * which reads and writes it; this feature writes the other three through the public goal, reminder
 * and settings contracts.
 *
 * @property appVersionCode the app that wrote the file, kept to trace a problem with a file.
 * @property createdAt ISO-8601 in UTC.
 */
@Serializable
internal data class BackupFileDto(
    val format: String,
    val formatVersion: Int,
    val appVersionCode: Long,
    val createdAt: String,
    val sections: Map<String, JsonObject>
) {
    companion object {
        const val FORMAT = "gil-backup"
        const val FORMAT_VERSION = 1

        const val PROGRESS = "progress"
        const val GOAL = "goal"
        const val REMINDERS = "reminders"
        const val SETTINGS = "settings"
        val SECTIONS = setOf(PROGRESS, GOAL, REMINDERS, SETTINGS)
    }
}

/** The daily goal in minutes, one of the lengths the app offers. */
@Serializable
internal data class GoalSectionDto(val version: Int, val minutes: Int)

/** Reminder times as `HH:mm`, at most two and each once; empty when reminders are off. */
@Serializable
internal data class RemindersSectionDto(val version: Int, val times: List<String>)

@Serializable
internal data class SettingsSectionDto(
    val version: Int,
    val theme: String,
    val accent: String,
    val speed: String,
    val showHangulAid: Boolean
)

/** The only version each of the three sections has so far. */
internal const val SECTION_VERSION = 1
