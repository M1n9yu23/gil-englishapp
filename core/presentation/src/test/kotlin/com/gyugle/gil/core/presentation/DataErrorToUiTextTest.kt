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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.DataError
import org.junit.jupiter.api.Test

class DataErrorToUiTextTest {

    @Test
    fun toUiText_diskFull_asksToFreeSpace() {
        assertThat(DataError.Local.DISK_FULL.toUiText())
            .isEqualTo(UiText.StringResource(R.string.common_error_disk_full))
    }

    @Test
    fun toUiText_unknown_genericStorageMessage() {
        assertThat(DataError.Local.UNKNOWN.toUiText())
            .isEqualTo(UiText.StringResource(R.string.common_error_storage))
    }
}
