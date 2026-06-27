package com.shikanoko.study.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Tracks how much wall-clock time a test/review session spent paused, so the displayed timer and the
// recorded session duration can both subtract it. Pure (no Compose / Android) so it is unit-testable;
// the clock is injected. Pauses are reference-counted by [depth]: overlapping pause sources (the
// manual pause button, the exit-warning dialog from the drawer, the stop dialog) collapse into one
// frozen interval, and time only resumes once every source has released.
class PauseClock(private val now: () -> Long = System::currentTimeMillis) {
    private var depth = 0
    private var accum = 0L
    private var mark = 0L

    val paused: Boolean get() = depth > 0

    fun pause() {
        if (depth++ == 0) mark = now()
    }

    fun resume() {
        if (depth > 0 && --depth == 0) accum += now() - mark
    }

    // Total paused milliseconds so far, including a still-open pause interval.
    fun pausedMillis(): Long = accum + if (depth > 0) now() - mark else 0

    // Clears all accounting so the same clock can be reused for a fresh session.
    fun reset() {
        depth = 0
        accum = 0L
        mark = 0L
    }
}

// Shared, Compose-observable handle that connects the drawer (in MainNavigation) with the running
// test/review screen. Created once in MainNavigation and passed down: the screen marks itself active
// and registers a recorder; the drawer reads [active] to decide whether to warn before navigating
// away, and pauses the clock while the warning is shown.
class TestSession {
    // A live session is running (false on the summary screen / when no test is loaded).
    var active by mutableStateOf(false)
        private set

    // Manual pause via the header button; drives the on-screen pause overlay and the button icon.
    var userPaused by mutableStateOf(false)
        private set

    val clock = PauseClock()

    // Set by the active screen: persists the partial session before the drawer navigates away.
    // Idempotent / null-safe — the screen skips recording if it has already finished.
    var recorder: (suspend () -> Unit)? = null

    fun begin() {
        active = true
        userPaused = false
        recorder = null
        clock.reset()
    }

    fun end() {
        active = false
        userPaused = false
        recorder = null
    }

    // Header pause button: toggles the manual pause and freezes/unfreezes the clock with it.
    fun toggleUserPause() {
        if (userPaused) {
            userPaused = false
            clock.resume()
        } else {
            userPaused = true
            clock.pause()
        }
    }

    // Freeze the clock while a dialog (drawer exit-warning, or stop confirmation) is shown.
    fun pushDialogPause() = clock.pause()
    fun popDialogPause() = clock.resume()
}
