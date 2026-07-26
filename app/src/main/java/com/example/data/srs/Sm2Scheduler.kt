package com.example.data.srs

import com.example.data.db.entity.RevisionNoteEntity
import java.util.concurrent.TimeUnit

enum class ReviewGrade(val label: String) {
    AGAIN("Encore"),
    HARD("Difficile"),
    GOOD("Bien"),
    EASY("Facile")
}

object Sm2Scheduler {

    fun schedule(note: RevisionNoteEntity, grade: ReviewGrade): RevisionNoteEntity {
        var ease = note.easeFactor
        var interval = note.intervalDays
        var reps = note.repetitions

        when (grade) {
            ReviewGrade.AGAIN -> {
                reps = 0
                interval = 1
                ease = (ease - 0.2f).coerceAtLeast(1.3f)
            }
            ReviewGrade.HARD -> {
                reps += 1
                interval = (interval * 1.2f).toInt().coerceAtLeast(1)
                ease = (ease - 0.15f).coerceAtLeast(1.3f)
            }
            ReviewGrade.GOOD -> {
                reps += 1
                interval = when (reps) {
                    1 -> 1
                    2 -> 6
                    else -> (interval * ease).toInt().coerceAtLeast(1)
                }
            }
            ReviewGrade.EASY -> {
                reps += 1
                interval = when (reps) {
                    1 -> 4
                    else -> (interval * ease * 1.3f).toInt().coerceAtLeast(1)
                }
                ease += 0.15f
            }
        }

        val nowMs = System.currentTimeMillis()
        val nextDueMs = nowMs + TimeUnit.DAYS.toMillis(interval.toLong())

        return note.copy(
            easeFactor = ease,
            intervalDays = interval,
            repetitions = reps,
            dueDate = nextDueMs,
            lastReviewedAt = nowMs,
            updatedAt = nowMs
        )
    }
}
