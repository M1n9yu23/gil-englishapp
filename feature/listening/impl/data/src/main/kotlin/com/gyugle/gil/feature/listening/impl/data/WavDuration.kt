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
package com.gyugle.gil.feature.listening.impl.data

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

private const val RIFF_HEADER_BYTES = 12L
private const val CHUNK_HEADER_BYTES = 8L

/** Where the byte rate sits in a `fmt ` chunk: after the format, channels and sample rate. */
private const val BYTE_RATE_OFFSET = 8L
private const val MICROS_PER_SECOND = 1_000_000L

/**
 * How long a WAV file plays, from its header, or null when it is not a WAV file the header can
 * tell. The player needs it before it opens the file to join the pieces of a track.
 */
internal fun wavDuration(file: File): Duration? = try {
    RandomAccessFile(file, "r").use { wav ->
        if (wav.length() < RIFF_HEADER_BYTES || wav.readTag() != "RIFF") return null
        wav.seek(RIFF_HEADER_BYTES - 4)
        if (wav.readTag() != "WAVE") return null
        var byteRate: Long? = null
        var position = RIFF_HEADER_BYTES
        while (position + CHUNK_HEADER_BYTES <= wav.length()) {
            wav.seek(position)
            val tag = wav.readTag()
            val size = wav.readLittleEndianInt()
            val dataStart = position + CHUNK_HEADER_BYTES
            when (tag) {
                "fmt " -> {
                    wav.seek(dataStart + BYTE_RATE_OFFSET)
                    byteRate = wav.readLittleEndianInt()
                }

                "data" -> {
                    val rate = byteRate?.takeIf { it > 0 } ?: return null
                    // An engine that streams may leave the size unset; the rest of the file is the sound.
                    val available = wav.length() - dataStart
                    val bytes = if (size in 1..available) size else available
                    return (bytes * MICROS_PER_SECOND / rate).microseconds
                }
            }
            // Chunks are padded to an even size.
            position = dataStart + size + (size and 1)
        }
        null
    }
} catch (_: IOException) {
    null
}

private fun RandomAccessFile.readTag(): String {
    val bytes = ByteArray(4)
    readFully(bytes)
    return String(bytes, Charsets.US_ASCII)
}

private fun RandomAccessFile.readLittleEndianInt(): Long {
    val bytes = ByteArray(4)
    readFully(bytes)
    return bytes.foldIndexed(0L) { index, value, byte -> value or ((byte.toLong() and 0xFF) shl (8 * index)) }
}
