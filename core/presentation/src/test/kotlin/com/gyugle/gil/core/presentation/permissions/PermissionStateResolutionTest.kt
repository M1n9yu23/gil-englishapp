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
package com.gyugle.gil.core.presentation.permissions

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PermissionStateResolutionTest {

    @ParameterizedTest(name = "granted={0}, rationale={1} -> {2}")
    @CsvSource(
        "true, false, GRANTED",
        "true, true, GRANTED",
        "false, true, DENIED",
        "false, false, PERMANENTLY_DENIED"
    )
    fun resolveStateAfterRequest_requestResult_mapsToState(
        granted: Boolean,
        showRationale: Boolean,
        expected: PermissionState
    ) {
        assertThat(resolveStateAfterRequest(granted, showRationale)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "granted={0}, rationale={1} -> {2}")
    @CsvSource(
        "true, false, GRANTED",
        "true, true, GRANTED",
        "false, true, DENIED",
        "false, false, NOT_DETERMINED"
    )
    fun resolveQueriedState_queryResult_mapsToState(
        granted: Boolean,
        showRationale: Boolean,
        expected: PermissionState
    ) {
        assertThat(resolveQueriedState(granted, showRationale)).isEqualTo(expected)
    }
}
