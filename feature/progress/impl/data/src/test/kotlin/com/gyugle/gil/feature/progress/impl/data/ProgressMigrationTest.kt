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
package com.gyugle.gil.feature.progress.impl.data

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Builds each old version from its committed schema, migrates it to the current version and
 * checks that Room accepts the result and the rows survive.
 *
 * When adding version N, write rows in the shape of version N - 1 in [writeRowsAt].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ProgressMigrationTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun everyOlderVersion_migratesToTheCurrentSchema() {
        for (version in 1 until ProgressDatabase.VERSION) {
            val helper = helperFor("migrate-from-$version.db")
            helper.createDatabase(version).use { writeRowsAt(version, it) }

            helper.runMigrationsAndValidate(ProgressDatabase.VERSION, ProgressDatabase.MIGRATIONS.toList()).close()
        }
    }

    @Test
    fun currentVersion_opensDatabaseWrittenAtEveryVersion_andKeepsTheRows() = runTest {
        for (version in 1..ProgressDatabase.VERSION) {
            val name = "open-from-$version.db"
            helperFor(name).createDatabase(version).use { writeRowsAt(version, it) }

            val database = Room.databaseBuilder(context, ProgressDatabase::class.java, name)
                .addMigrations(*ProgressDatabase.MIGRATIONS)
                .build()
            try {
                val cards = database.cardDao().getAll().map { it.itemId }
                assertThat(cards).isEqualTo(listOf("sense.hello.interj.greeting"))
                val studyTime = database.studyTimeDao().getAll().map { it.durationMillis }
                assertThat(studyTime).isEqualTo(listOf(300_000L))
            } finally {
                database.close()
            }
        }
    }

    private fun helperFor(name: String) = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context.getDatabasePath(name),
        driver = AndroidSQLiteDriver(),
        databaseClass = ProgressDatabase::class
    )

    private fun writeRowsAt(version: Int, connection: SQLiteConnection) {
        when (version) {
            1 -> {
                connection.execSQL(
                    "INSERT INTO card (itemId, stability, difficulty, dueDay, lastReviewDay, lastReviewAt, " +
                        "reviewCount, lapseCount, introducedAt) " +
                        "VALUES ('sense.hello.interj.greeting', 3.17, 2.118, 20400, 20397, 1762341600000, 1, 0, 1762341600000)"
                )
                connection.execSQL("INSERT INTO study_time (studyDay, durationMillis) VALUES (20397, 300000)")
            }

            else -> error("Write rows for version $version")
        }
    }
}
