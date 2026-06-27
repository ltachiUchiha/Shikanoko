package com.shikanoko.study.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shikanoko.study.R
import com.shikanoko.study.data.ReviewSession
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.buildReviewSession
import com.shikanoko.study.data.isNew
import com.shikanoko.study.data.persistReview
import com.shikanoko.study.data.recordAnswer
import com.shikanoko.study.data.recordSession
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.withKana
import com.shikanoko.study.data.srs.AnswerCheck
import com.shikanoko.study.data.srs.AutoGrade
import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.DailyCounters
import com.shikanoko.study.data.srs.Grade
import com.shikanoko.study.data.srs.ReviewCandidate
import com.shikanoko.study.data.srs.SchedulerState
import com.shikanoko.study.data.srs.Sm2Scheduler
import com.shikanoko.study.ui.components.AnswerBanner
import com.shikanoko.study.ui.components.FEEDBACK_MS
import com.shikanoko.study.ui.components.KanaColumn
import com.shikanoko.study.ui.components.PauseOverlay
import com.shikanoko.study.ui.components.PromptText
import com.shikanoko.study.ui.components.SessionResults
import com.shikanoko.study.ui.components.StopConfirmDialog
import com.shikanoko.study.ui.components.TestSession
import com.shikanoko.study.ui.components.TestStatsHeader
import com.shikanoko.study.ui.components.TestSummary
import com.shikanoko.study.ui.components.buildOptions
import com.shikanoko.study.ui.destination.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// How far ahead a still-learning card is re-inserted so it reappears later in the same session
// (minute-scale steps only set relative order here; no real wall-clock waiting is enforced).
private const val REINSERT_GAP = 3

private val scheduler = Sm2Scheduler()

// The SRS Review test. The pre-test intro (today's progress + start/study-more) now lives on the
// Review entry screen (the settings dialog), so this builds the session straight away and runs it.
// [ignoreDailyLimit] is the "study more" path: today's done-counts are treated as 0 for a fresh batch.
@Composable
fun ReviewScreen(
    navController: NavController,
    args: MutableState<TestingSettings>,
    ignoreDailyLimit: Boolean,
    testSession: TestSession
) {
    val settings = args.value
    val context = LocalContext.current
    var session by remember { mutableStateOf<ReviewSession?>(null) }

    // Build the session once; an empty queue means nothing is due, so report it and go back.
    LaunchedEffect(Unit) {
        val built = withContext(Dispatchers.IO) {
            buildReviewSession(context, settings, ignoreDailyLimit)
        }
        if (built.queue.isEmpty()) {
            Toast.makeText(context, R.string.review_nothing_due, Toast.LENGTH_SHORT).show()
            navController.popBackStack(MainScreen.route, false)
        } else {
            session = built
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        val active = session
        if (active == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (settings.testType == TestType.CARD) {
            ReviewByCards(settings, active, testSession) { navController.popBackStack(MainScreen.route, false) }
        } else {
            ReviewByEnter(settings, active, testSession) { navController.popBackStack(MainScreen.route, false) }
        }
    }
}

@Composable
private fun ReviewByEnter(
    settings: TestingSettings,
    session: ReviewSession,
    testSession: TestSession,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val padding = 8.dp

    val queue = remember { mutableStateListOf<ReviewCandidate>().also { it.addAll(session.queue) } }
    var userValue by remember { mutableStateOf("") }
    // When the current card was first shown; response time vs. this drives the auto-grade.
    var shownAt by remember { mutableStateOf(System.currentTimeMillis()) }

    val state = remember { ReviewSessionState().also { it.total = session.queue.size } }
    val results = remember { SessionResults() }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showStopDialog by remember { mutableStateOf(false) }
    // Live hiragana toggle (seeded from settings): swaps kanji for kana on Meaning -> JP cards.
    var showKana by remember { mutableStateOf(settings.showKana) }
    val hasMeaningToJp = Direction.MEANING_TO_JP in
        settings.directions.ifEmpty { setOf(Direction.JP_TO_MEANING) }

    // Feedback state for the current card.
    var reveal by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf(false) }
    // When a typed answer is wrong we pause for the user to read it and optionally override.
    var awaitingDecision by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }

    // Mark the session live and register how to persist a partial run if the user leaves via the
    // drawer. Idempotent: skipped once the review has finished (guards against a double record).
    LaunchedEffect(Unit) {
        testSession.begin()
        testSession.recorder = recorder@{
            if (state.finished) return@recorder
            val durationMillis = System.currentTimeMillis() - state.startMillis - testSession.clock.pausedMillis()
            withContext(Dispatchers.IO) {
                recordSession(context, durationMillis, state.total, state.attempts, state.correct)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { testSession.end() } }

    LaunchedEffect(state.finished) {
        while (!state.finished) {
            elapsedSeconds = (System.currentTimeMillis() - state.startMillis - testSession.clock.pausedMillis()) / 1000
            delay(1000)
        }
        // Finished (naturally or via early stop): drop active so the drawer stops intercepting.
        if (state.finished) testSession.end()
    }

    if (state.finished) {
        TestSummary(state.finalElapsed, state.total, accuracyPercent(state.correct, state.attempts), results.snapshot()) {
            onExit()
        }
        return
    }

    val current = queue.first()

    // Commits the auto-derived [grade], advances the queue, and resets the per-card feedback.
    fun proceed(grade: Grade) {
        scope.launch {
            val correct = grade != Grade.AGAIN
            if (correct) state.correct++
            results.record(current.studyWord, correct)
            val newState = withContext(Dispatchers.IO) { commitGrade(context, current, grade) }
            advanceQueue(context, queue, current, newState, state, testSession.clock.pausedMillis())
            reveal = false
            awaitingDecision = false
            userValue = ""
            locked = false
            shownAt = System.currentTimeMillis()
        }
    }

    // Answer resolved for the current kana/kanji choice (Meaning -> JP only; no-op otherwise).
    val currentAnswer = current.studyWord.withKana(showKana).answer

    BackHandler(enabled = !state.finished) { showStopDialog = true }
    if (showStopDialog) {
        // Freeze the timer while the confirmation is shown, mirroring the drawer exit-warning.
        DisposableEffect(Unit) {
            testSession.pushDialogPause()
            onDispose { testSession.popDialogPause() }
        }
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; scope.launch { stopReview(context, state, testSession.clock.pausedMillis()) } },
            onDismiss = { showStopDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .padding(padding)
    ) {
        TestStatsHeader(
            elapsedSeconds, state.completed, state.total,
            onStop = { showStopDialog = true },
            progressLabel = stringResource(R.string.review_mastered, state.completed, state.total),
            paused = testSession.userPaused,
            pauseEnabled = !locked,
            onTogglePause = { testSession.toggleUserPause() }
        )
        if (hasMeaningToJp) KanaToggle(showKana) { showKana = it }
        Spacer(Modifier.size(padding))
        PromptText(current.studyWord)
        AnswerBanner(reveal, lastCorrect, currentAnswer)
        Spacer(Modifier.size(padding))

        OutlinedTextField(
            value = userValue,
            singleLine = true,
            enabled = !locked,
            onValueChange = { userValue = it },
            label = { Text(stringResource(id = R.string.db_meaning_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(padding))

        if (awaitingDecision) {
            // Wrong answer: let the user continue (a lapse) or claim the normalization was too strict.
            // "I was right" means it was recalled but only after a near-miss, so it grades as HARD.
            Row(horizontalArrangement = Arrangement.spacedBy(padding)) {
                Button(onClick = { proceed(Grade.AGAIN) }) {
                    Text(stringResource(R.string.review_continue))
                }
                OutlinedButton(onClick = { proceed(Grade.HARD) }) {
                    Text(stringResource(R.string.review_i_was_right))
                }
            }
        } else {
            Button(
                enabled = !locked,
                onClick = {
                    if (locked) return@Button
                    val correct = AnswerCheck.isCorrect(currentAnswer, userValue)
                    state.attempts++
                    reveal = true
                    lastCorrect = correct
                    locked = true
                    if (correct) {
                        // Grade from how quickly the answer was typed; capture now, before the delay.
                        val grade = AutoGrade.fromTiming(
                            true, System.currentTimeMillis() - shownAt,
                            AutoGrade.TEXT_EASY_MAX_MS, AutoGrade.TEXT_HARD_MIN_MS
                        )
                        scope.launch {
                            delay(FEEDBACK_MS)
                            proceed(grade)
                        }
                    } else {
                        awaitingDecision = true
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.testing_check_btn))
            }
        }
    }
        if (testSession.userPaused) {
            PauseOverlay(onResume = { testSession.toggleUserPause() })
        }
    }
}

@Composable
private fun ReviewByCards(
    settings: TestingSettings,
    session: ReviewSession,
    testSession: TestSession,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pools = remember { session.pools }
    fun optionsFor(candidate: ReviewCandidate): List<StudyWord> =
        buildOptions(pools[candidate.direction].orEmpty(), candidate.studyWord)

    val queue = remember { mutableStateListOf<ReviewCandidate>().also { it.addAll(session.queue) } }
    var currentWords by remember { mutableStateOf(optionsFor(queue.first())) }
    // When the current card was first shown; response time vs. this drives the auto-grade.
    var shownAt by remember { mutableStateOf(System.currentTimeMillis()) }

    val state = remember { ReviewSessionState().also { it.total = session.queue.size } }
    val results = remember { SessionResults() }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showStopDialog by remember { mutableStateOf(false) }
    // Live hiragana toggle (seeded from settings): swaps kanji for kana on Meaning -> JP cards.
    var showKana by remember { mutableStateOf(settings.showKana) }
    val hasMeaningToJp = Direction.MEANING_TO_JP in
        settings.directions.ifEmpty { setOf(Direction.JP_TO_MEANING) }

    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var reveal by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }

    // Mark the session live and register how to persist a partial run if the user leaves via the
    // drawer. Idempotent: skipped once the review has finished (guards against a double record).
    LaunchedEffect(Unit) {
        testSession.begin()
        testSession.recorder = recorder@{
            if (state.finished) return@recorder
            val durationMillis = System.currentTimeMillis() - state.startMillis - testSession.clock.pausedMillis()
            withContext(Dispatchers.IO) {
                recordSession(context, durationMillis, state.total, state.attempts, state.correct)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { testSession.end() } }

    LaunchedEffect(state.finished) {
        while (!state.finished) {
            elapsedSeconds = (System.currentTimeMillis() - state.startMillis - testSession.clock.pausedMillis()) / 1000
            delay(1000)
        }
        // Finished (naturally or via early stop): drop active so the drawer stops intercepting.
        if (state.finished) testSession.end()
    }

    if (state.finished) {
        TestSummary(state.finalElapsed, state.total, accuracyPercent(state.correct, state.attempts), results.snapshot()) {
            onExit()
        }
        return
    }

    val current = queue.first()
    // Resolve the kanji/kana choice once per recomposition; the option buttons render and are
    // compared against these so toggling hiragana just swaps the displayed text.
    val currentAnswer = current.studyWord.withKana(showKana).answer
    val displayWords = currentWords.map { it.withKana(showKana) }

    val onKanaButtonClick: (String) -> Unit = onClick@{ answer ->
        if (locked) return@onClick
        val correct = answer == currentAnswer
        locked = true
        reveal = true
        selectedAnswer = answer
        state.attempts++
        if (correct) state.correct++
        results.record(current.studyWord, correct)
        // Grade from how quickly the choice was tapped; capture now, before the feedback delay.
        val grade = AutoGrade.fromTiming(
            correct, System.currentTimeMillis() - shownAt,
            AutoGrade.CARD_EASY_MAX_MS, AutoGrade.CARD_HARD_MIN_MS
        )
        scope.launch {
            val newState = withContext(Dispatchers.IO) { commitGrade(context, current, grade) }
            delay(FEEDBACK_MS)
            advanceQueue(context, queue, current, newState, state, testSession.clock.pausedMillis())
            reveal = false
            selectedAnswer = null
            if (!state.finished) currentWords = optionsFor(queue.first())
            locked = false
            shownAt = System.currentTimeMillis()
        }
    }

    BackHandler(enabled = !state.finished) { showStopDialog = true }
    if (showStopDialog) {
        // Freeze the timer while the confirmation is shown, mirroring the drawer exit-warning.
        DisposableEffect(Unit) {
            testSession.pushDialogPause()
            onDispose { testSession.popDialogPause() }
        }
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; scope.launch { stopReview(context, state, testSession.clock.pausedMillis()) } },
            onDismiss = { showStopDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
            .padding(8.dp)
    ) {
        TestStatsHeader(
            elapsedSeconds, state.completed, state.total,
            onStop = { showStopDialog = true },
            progressLabel = stringResource(R.string.review_mastered, state.completed, state.total),
            paused = testSession.userPaused,
            pauseEnabled = !locked,
            onTogglePause = { testSession.toggleUserPause() }
        )
        if (hasMeaningToJp) KanaToggle(showKana) { showKana = it }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PromptText(current.studyWord)
            Spacer(Modifier.padding(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
            ) {
                if (displayWords.isNotEmpty()) {
                    KanaColumn(0.5f, displayWords.take(3), currentAnswer, selectedAnswer, reveal, locked, onKanaButtonClick)
                    KanaColumn(1f, displayWords.drop(3), currentAnswer, selectedAnswer, reveal, locked, onKanaButtonClick)
                }
            }
        }
    }
        if (testSession.userPaused) {
            PauseOverlay(onResume = { testSession.toggleUserPause() })
        }
    }
}

// Right-aligned hiragana switch shown on the Review screen when Meaning -> JP cards are in play.
@Composable
private fun KanaToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.review_show_kana),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.size(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Mutable per-session metrics, kept in one holder so the grading helpers can update them. Fields
// read by the UI are Compose state so changes (notably [finished]) trigger recomposition.
private class ReviewSessionState {
    val startMillis = System.currentTimeMillis()
    var total by mutableStateOf(0)          // cards in the initial session
    var completed by mutableStateOf(0)      // cards that graduated/left the session (progress bar)
    var attempts by mutableStateOf(0)
    var correct by mutableStateOf(0)
    var finished by mutableStateOf(false)
    var finalElapsed by mutableStateOf(0L)
}

// Grades [candidate] with an auto-derived [grade], persists the new card state, records the answer
// stat, and bumps the right daily counter. Returns the new scheduler state. Blocking — call inside
// withContext(Dispatchers.IO). AGAIN is the only "wrong" outcome, so the answer stat is correct iff
// the grade is anything else.
private suspend fun commitGrade(
    context: Context,
    candidate: ReviewCandidate,
    grade: Grade
): SchedulerState {
    val now = System.currentTimeMillis()
    val correct = grade != Grade.AGAIN
    val prev = candidate.state ?: scheduler.initialState(now)
    val newState = scheduler.review(prev, grade, now)
    persistReview(context, candidate, newState)
    recordAnswer(context, candidate.studyWord, correct)
    val counters = DailyCounters(context)
    when {
        candidate.isNew -> counters.recordNew()
        // Only genuine day-scale reviews count against the review limit; learning/relearning
        // re-shows within a session don't.
        candidate.state?.phase == CardPhase.REVIEW -> counters.recordReview()
    }
    return newState
}

// Removes the just-graded card from the front of the queue. A card that graduated to day-scale
// REVIEW leaves the session; one still in learning/relearning is re-inserted to reappear later.
// Records the TestSession (for the Statistics totals) and finishes when the queue empties.
private suspend fun advanceQueue(
    context: Context,
    queue: MutableList<ReviewCandidate>,
    current: ReviewCandidate,
    newState: SchedulerState,
    state: ReviewSessionState,
    pausedMillis: Long
) {
    queue.removeAt(0)
    val stillLearning = newState.phase == CardPhase.LEARNING || newState.phase == CardPhase.RELEARNING
    if (stillLearning) {
        queue.add(minOf(queue.size, REINSERT_GAP), current.copy(state = newState))
    } else {
        state.completed++
    }
    if (queue.isEmpty()) {
        val durationMillis = System.currentTimeMillis() - state.startMillis - pausedMillis
        state.finalElapsed = durationMillis / 1000
        // Flip to the summary before the suspending DB write so no recomposition can read an empty
        // queue while still "unfinished".
        state.finished = true
        withContext(Dispatchers.IO) {
            recordSession(context, durationMillis, state.total, state.attempts, state.correct)
        }
    }
}

// Ends the session early (X button / system back): records the partial run and flips to the summary,
// mirroring advanceQueue's finish path but without waiting for the queue to empty.
private suspend fun stopReview(context: Context, state: ReviewSessionState, pausedMillis: Long) {
    if (state.finished) return
    val durationMillis = System.currentTimeMillis() - state.startMillis - pausedMillis
    state.finalElapsed = durationMillis / 1000
    state.finished = true
    withContext(Dispatchers.IO) {
        recordSession(context, durationMillis, state.total, state.attempts, state.correct)
    }
}
