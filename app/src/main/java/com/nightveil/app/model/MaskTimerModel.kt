package com.nightveil.app.model

object MaskTimerModel {
    val OPTIONS = listOf(0, 15, 30, 60)

    fun deadline(minutes: Int, now: Long): Long =
        if (minutes in OPTIONS && minutes > 0) now + minutes * 60_000L else 0L

    fun remainingMinutes(deadline: Long, now: Long): Int =
        ((deadline - now).coerceAtLeast(0L).plus(59_999L) / 60_000L).toInt()

    fun hasExpired(deadline: Long, now: Long): Boolean = deadline > 0 && now >= deadline
}
