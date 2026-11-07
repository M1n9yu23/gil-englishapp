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
package com.gyugle.gil.feature.curriculum.impl.data

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/**
 * While [blocked], puts every database inside a regular file, where no directory can be made, so
 * copying the asset fails. Unblocked, it behaves like [base].
 */
class BlockableDatabaseContext(base: Context) : ContextWrapper(base) {
    var blocked = true

    private val blocker = File(base.cacheDir, "not-a-directory").apply {
        parentFile?.mkdirs()
        writeText("")
    }

    override fun getApplicationContext(): Context = this

    override fun getDatabasePath(name: String): File =
        if (blocked) File(blocker, name) else baseContext.getDatabasePath(name)
}
