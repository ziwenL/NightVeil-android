package com.nightveil.app.model

import org.junit.Assert.*
import org.junit.Test

class MaskTimerModelTest {
    @Test fun `disabled and unsupported durations do not create deadlines`() {
        assertEquals(0L, MaskTimerModel.deadline(0, 100L))
        assertEquals(0L, MaskTimerModel.deadline(-1, 100L))
        assertEquals(0L, MaskTimerModel.deadline(999, 100L))
    }

    @Test fun `each preset schedules from the current monotonic time`() {
        for (minutes in listOf(15, 30, 60)) {
            assertEquals(123_456L + minutes * 60_000L, MaskTimerModel.deadline(minutes, 123_456L))
        }
    }

    @Test fun `remaining minutes round up and never become negative`() {
        assertEquals(15, MaskTimerModel.remainingMinutes(900_100L, 100L))
        assertEquals(1, MaskTimerModel.remainingMinutes(900_100L, 900_099L))
        assertEquals(0, MaskTimerModel.remainingMinutes(900_100L, 900_100L))
        assertEquals(0, MaskTimerModel.remainingMinutes(900_100L, 999_999L))
    }

    @Test fun `expiry includes the deadline and ignores disabled timers`() {
        assertFalse(MaskTimerModel.hasExpired(0L, 100L))
        assertFalse(MaskTimerModel.hasExpired(100L, 99L))
        assertTrue(MaskTimerModel.hasExpired(100L, 100L))
        assertTrue(MaskTimerModel.hasExpired(100L, 101L))
    }
}
