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
package com.gyugle.gil.feature.curriculum.impl.data.dto

import kotlinx.serialization.Serializable

/**
 * One element of `sentence.tokens`, written by tools/content-compiler (`TokenJson`). [word] is null
 * for a word the dictionary does not have, such as a name.
 */
@Serializable
data class TokenDto(val start: Int, val end: Int, val word: String? = null, val sense: String? = null)

/** One element of `sentence.chunks`, written by tools/content-compiler (`ChunkJson`). */
@Serializable
data class ChunkDto(val start: Int, val end: Int, val role: String)
