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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class OutcomeTest {

    private enum class TestError : RootError { FIRST, SECOND }

    private enum class OtherError : RootError { MAPPED }

    private val success: Outcome<Int, TestError> = Outcome.Success(2)
    private val failure: Outcome<Int, TestError> = Outcome.Failure(TestError.FIRST)

    @Test
    fun map_success_transformsValue() {
        assertThat(success.map { it * 10 }).isEqualTo(Outcome.Success(20))
    }

    @Test
    fun map_failure_keepsErrorAndSkipsTransform() {
        var called = false
        val result = failure.map {
            called = true
            it * 10
        }

        assertThat(result).isEqualTo(Outcome.Failure(TestError.FIRST))
        assertThat(called).isFalse()
    }

    @Test
    fun flatMap_successThenSuccess_returnsSecondValue() {
        assertThat(success.flatMap { Outcome.Success("v$it") }).isEqualTo(Outcome.Success("v2"))
    }

    @Test
    fun flatMap_successThenFailure_returnsSecondError() {
        val result = success.flatMap<Int, TestError, String> { Outcome.Failure(TestError.SECOND) }

        assertThat(result).isEqualTo(Outcome.Failure(TestError.SECOND))
    }

    @Test
    fun flatMap_failure_keepsFirstErrorAndSkipsSecondStep() {
        var called = false
        val result = failure.flatMap {
            called = true
            Outcome.Success(it)
        }

        assertThat(result).isEqualTo(Outcome.Failure(TestError.FIRST))
        assertThat(called).isFalse()
    }

    @Test
    fun mapError_failure_translatesError() {
        assertThat(failure.mapError { OtherError.MAPPED }).isEqualTo(Outcome.Failure(OtherError.MAPPED))
    }

    @Test
    fun mapError_success_keepsValue() {
        assertThat(success.mapError { OtherError.MAPPED }).isEqualTo(Outcome.Success(2))
    }

    @Test
    fun fold_success_usesSuccessBranch() {
        assertThat(success.fold(onSuccess = { "ok $it" }, onFailure = { "error $it" })).isEqualTo("ok 2")
    }

    @Test
    fun fold_failure_usesFailureBranch() {
        assertThat(failure.fold(onSuccess = { "ok $it" }, onFailure = { "error $it" })).isEqualTo("error FIRST")
    }

    @Test
    fun getOrNull_success_returnsValue() {
        assertThat(success.getOrNull()).isEqualTo(2)
    }

    @Test
    fun getOrNull_failure_returnsNull() {
        assertThat(failure.getOrNull()).isNull()
    }

    @Test
    fun onSuccess_success_runsActionAndReturnsSameOutcome() {
        val seen = mutableListOf<Int>()

        val result = success.onSuccess { seen += it }

        assertThat(seen).isEqualTo(listOf(2))
        assertThat(result).isEqualTo(success)
    }

    @Test
    fun onSuccess_failure_skipsAction() {
        val seen = mutableListOf<Int>()

        failure.onSuccess { seen += it }

        assertThat(seen).isEqualTo(emptyList())
    }

    @Test
    fun onFailure_failure_runsActionAndReturnsSameOutcome() {
        val seen = mutableListOf<TestError>()

        val result = failure.onFailure { seen += it }

        assertThat(seen).isEqualTo(listOf(TestError.FIRST))
        assertThat(result).isEqualTo(failure)
    }

    @Test
    fun onFailure_success_skipsAction() {
        val seen = mutableListOf<TestError>()

        success.onFailure { seen += it }

        assertThat(seen).isEqualTo(emptyList())
    }

    @Test
    fun asEmptyOutcome_success_dropsValue() {
        assertThat(success.asEmptyOutcome()).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun asEmptyOutcome_failure_keepsError() {
        assertThat(failure.asEmptyOutcome()).isEqualTo(Outcome.Failure(TestError.FIRST))
    }
}
