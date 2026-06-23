package com.shikanoko.study

import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.formatElapsed
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTest {

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:09", formatElapsed(9))
        assertEquals("01:05", formatElapsed(65))
        assertEquals("59:59", formatElapsed(3599))
    }

    @Test
    fun formatsHoursOncePastAnHour() {
        assertEquals("1:00:00", formatElapsed(3600))
        assertEquals("2:03:04", formatElapsed(7384))
    }

    @Test
    fun negativeElapsedClampsToZero() {
        assertEquals("00:00", formatElapsed(-5))
    }

    @Test
    fun accuracyRoundsToNearestPercent() {
        assertEquals(0, accuracyPercent(0, 0))   // nothing seen
        assertEquals(0, accuracyPercent(0, 4))
        assertEquals(50, accuracyPercent(1, 2))
        assertEquals(100, accuracyPercent(3, 3))
        assertEquals(67, accuracyPercent(2, 3))  // 66.67 -> 67
    }
}
