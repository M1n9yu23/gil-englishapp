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
package com.gyugle.gil.core.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import java.net.URLEncoder
import timber.log.Timber

private const val NEW_ISSUE_URL = "https://github.com/M1n9yu23/gil-englishapp/issues/new"

/**
 * The address of the project's form for reporting a mistake in learning content, with the fields
 * filled in: [location] is the content ID where the learner saw it, [shown] the text as it
 * appears. The app has no network access, so the browser sends the report, not the app.
 */
fun contentReportUrl(location: String, appVersion: String, shown: String?, korean: Boolean): String {
    val fields = buildList {
        add("template" to if (korean) "content_error.ko.yml" else "content_error.yml")
        add("location" to location)
        add("app-version" to appVersion)
        if (shown != null) add("shown" to shown)
    }
    return fields.joinToString(separator = "&", prefix = "$NEW_ISSUE_URL?") { (name, value) ->
        "$name=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
    }
}

/** Opens the content report form in the browser. [canReport] is false on a phone without one. */
@Stable
class ContentReporter internal constructor(private val context: Context, private val korean: Boolean) {
    /** Asked once: screens read it on every recomposition, and the answer is a system call. */
    val canReport: Boolean = Intent(Intent.ACTION_VIEW, NEW_ISSUE_URL.toUri())
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .resolveActivity(context.packageManager) != null

    fun report(location: String, shown: String?) {
        val url = contentReportUrl(location, appVersion(), shown, korean)
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // The browser went away after the menu was shown; nothing else can open the form.
            Timber.w(e, "No browser for the content report")
        }
    }

    private fun appVersion(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}

/** A [ContentReporter] that fills in the form in the app's language. */
@Composable
fun rememberContentReporter(): ContentReporter {
    val context = LocalContext.current
    val korean = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language == "ko"
    return remember(context, korean) { ContentReporter(context, korean) }
}
