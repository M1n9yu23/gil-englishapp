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
package com.gyugle.gil.feature.curriculum.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/** Routes are saved with the back stack; the saved form must stay readable across app updates. */
class CurriculumRouteTest {
    @Test
    fun unitRoute_savedAndRead_keepsTheIdAsPlainString() {
        val route: CurriculumRoute = UnitRoute(UnitId("unit.a1-03"))

        val saved = Json.encodeToString(CurriculumRoute.serializer(), route)

        assertThat(saved).isEqualTo("""{"type":"curriculum.unit","unitId":"unit.a1-03"}""")
        assertThat(Json.decodeFromString(CurriculumRoute.serializer(), saved)).isEqualTo(route)
    }
}
