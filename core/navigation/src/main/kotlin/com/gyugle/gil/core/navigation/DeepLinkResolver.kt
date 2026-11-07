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
package com.gyugle.gil.core.navigation

/**
 * Turns the URI of an outside entry (a notification, the widget, a launcher shortcut) into a
 * [DeepLinkTarget].
 *
 * The URI is untrusted input: any app can start the activity with any data. Check its shape and
 * every id in it here, and return null for anything unexpected; the caller then opens the start
 * tab. Never read more than ids from it.
 */
fun interface DeepLinkResolver {
    fun resolve(uri: String): DeepLinkTarget?
}
