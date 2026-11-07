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
package com.gyugle.gil.core.domain

/**
 * The result of an operation that can fail in a way the caller must handle.
 *
 * Named to stay clear of `kotlin.Result`: its default-imported `map`, `onSuccess` and `fold`
 * would otherwise compile against the wrong type.
 */
sealed interface Outcome<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Outcome<D, Nothing>

    data class Failure<out E : RootError>(val error: E) : Outcome<Nothing, E>
}

typealias EmptyOutcome<E> = Outcome<Unit, E>

inline fun <T, E : RootError, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

inline fun <T, E : RootError, R> Outcome<T, E>.flatMap(transform: (T) -> Outcome<R, E>): Outcome<R, E> = when (this) {
    is Outcome.Success -> transform(data)
    is Outcome.Failure -> this
}

inline fun <T, E : RootError, F : RootError> Outcome<T, E>.mapError(transform: (E) -> F): Outcome<T, F> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Failure(transform(error))
}

inline fun <T, E : RootError, R> Outcome<T, E>.fold(onSuccess: (T) -> R, onFailure: (E) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(data)
    is Outcome.Failure -> onFailure(error)
}

fun <T, E : RootError> Outcome<T, E>.getOrNull(): T? = when (this) {
    is Outcome.Success -> data
    is Outcome.Failure -> null
}

/** Runs [action] on success and returns the same outcome. It cannot replace the value; use [fold] for a fallback. */
inline fun <T, E : RootError> Outcome<T, E>.onSuccess(action: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Success) action(data)
    return this
}

/** Runs [action] on failure and returns the same outcome. */
inline fun <T, E : RootError> Outcome<T, E>.onFailure(action: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Failure) action(error)
    return this
}

fun <T, E : RootError> Outcome<T, E>.asEmptyOutcome(): EmptyOutcome<E> = map { }
