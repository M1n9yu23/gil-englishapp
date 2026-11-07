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
package com.gyugle.gil.feature.progress.impl.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One rating and the schedule it produced, kept so the history can be replayed later, for
 * example to fit FSRS weights to this learner.
 *
 * @property rating the FSRS grade: 1 again, 2 hard, 3 good.
 */
@Entity(tableName = "review_log", indices = [Index("itemId"), Index("studyDay")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val rating: Int,
    val reviewedAt: Long,
    val studyDay: Long,
    val stability: Double,
    val difficulty: Double,
    val dueDay: Long
)
