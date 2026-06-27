package com.shikanoko.study

import com.shikanoko.study.ui.components.PauseClock
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseClockTest {

    // Drives the clock from the test so timing is deterministic.
    private var nowMs = 0L
    private val clock = PauseClock { nowMs }

    @Test
    fun noPauseIsZero() {
        nowMs = 5_000
        assertEquals(0L, clock.pausedMillis())
    }

    @Test
    fun singlePauseAccountsTheInterval() {
        nowMs = 100
        clock.pause()
        nowMs = 300
        clock.resume()
        nowMs = 1_000
        assertEquals(200L, clock.pausedMillis())
    }

    @Test
    fun pausedMillisGrowsWhileStillPaused() {
        nowMs = 100
        clock.pause()
        nowMs = 250
        assertEquals(150L, clock.pausedMillis())
        nowMs = 400
        assertEquals(300L, clock.pausedMillis())
    }

    @Test
    fun overlappingPausesCountAsOneInterval() {
        // Manual pause and a dialog pause overlap; time only resumes once both release.
        nowMs = 100
        clock.pause()   // depth 1
        nowMs = 150
        clock.pause()   // depth 2 (still the same frozen interval)
        nowMs = 300
        clock.resume()  // depth 1, not yet accounted
        nowMs = 500
        clock.resume()  // depth 0, accounts 100..500
        nowMs = 1_000
        assertEquals(400L, clock.pausedMillis())
    }

    @Test
    fun resumeWithoutPauseIsNoOp() {
        nowMs = 100
        clock.resume()
        nowMs = 500
        assertEquals(0L, clock.pausedMillis())
    }

    @Test
    fun resetClearsAccounting() {
        nowMs = 100
        clock.pause()
        nowMs = 300
        clock.resume()
        clock.reset()
        nowMs = 1_000
        assertEquals(0L, clock.pausedMillis())
    }
}
