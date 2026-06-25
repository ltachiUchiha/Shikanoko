package com.shikanoko.study.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shikanoko.study.R
import com.shikanoko.study.data.ReviewSession
import com.shikanoko.study.data.ReviewSettingsStore
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.buildReviewSession
import com.shikanoko.study.data.countScopeWords
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
import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.DailyCounters
import com.shikanoko.study.data.srs.Grade
import com.shikanoko.study.data.srs.ReviewCandidate
import com.shikanoko.study.data.srs.SchedulerState
import com.shikanoko.study.data.srs.Sm2Scheduler
import com.shikanoko.study.ui.components.AnswerBanner
import com.shikanoko.study.ui.components.FEEDBACK_MS
import com.shikanoko.study.ui.components.KanaColumn
import com.shikanoko.study.ui.components.PromptText
import com.shikanoko.study.ui.components.SessionResults
import com.shikanoko.study.ui.components.StopConfirmDialog
import com.shikanoko.study.ui.components.TestStatsHeader
import com.shikanoko.study.ui.components.TestSummary
import com.shikanoko.study.ui.components.buildOptions
import com.shikanoko.study.ui.destination.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

// How far ahead a still-learning card is re-inserted so it reappears later in the same session
// (minute-scale steps only set relative order here; no real wall-clock waiting is enforced).
private const val REINSERT_GAP = 3

private val scheduler = Sm2Scheduler()

// The two phases of the Review flow: the intro (today's progress + start/study-more) and the actual
// test once a session has been built.
private enum class ReviewPhase { INTRO, RUNNING }

// The SRS Review flow. Shows an intro with today's progress, then mirrors the practice
// TestingScreen's two modes — but the pool comes from the scheduler's due-queue and every answer
// advances the card's spaced-repetition state.
@Composable
fun ReviewScreen(navController: NavController, args: MutableState<TestingSettings>) {
    val settings = args.value
    var phase by remember { mutableStateOf(ReviewPhase.INTRO) }
    var session by remember { mutableStateOf<ReviewSession?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        when (phase) {
            ReviewPhase.INTRO -> ReviewIntro(
                settings = settings,
                onStart = { built -> session = built; phase = ReviewPhase.RUNNING },
                onClose = { navController.popBackStack(MainScreen.route, false) }
            )
            ReviewPhase.RUNNING -> {
                val active = session
                if (active == null) {
                    phase = ReviewPhase.INTRO
                } else {
                    // Finishing or stopping returns to the intro so the user can immediately study
                    // more, with the today's-progress counts refreshed.
                    val backToIntro = { session = null; phase = ReviewPhase.INTRO }
                    if (settings.testType == TestType.CARD)
                        ReviewByCards(settings, active, backToIntro)
                    else
                        ReviewByEnter(settings, active, backToIntro)
                }
            }
        }
    }
}

// Pre-test screen: shows how much has been studied today and offers a normal session (capped by the
// daily limit) or a "study more" session that ignores today's counts for another batch.
@Composable
private fun ReviewIntro(
    settings: TestingSettings,
    onStart: (ReviewSession) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newDone by remember { mutableStateOf(0) }
    var reviewsDone by remember { mutableStateOf(0) }
    var building by remember { mutableStateOf(false) }
    // Review plan: the whole scope's word count and the resulting estimate of days to cover it.
    var totalWords by remember { mutableStateOf(0) }
    var estimatedDays by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Reading the counters runs a midnight rollover (a prefs write); do it off the main thread.
        val (n, r) = withContext(Dispatchers.IO) {
            val counters = DailyCounters(context)
            counters.newDoneToday to counters.reviewsDoneToday
        }
        newDone = n
        reviewsDone = r

        // Count the scope's words, then estimate how many days the daily pace needs to cover them.
        val total = withContext(Dispatchers.IO) { countScopeWords(context, settings) }
        val daily = settings.maxNewPerDay.coerceAtLeast(1)
        val days = if (total > 0) ceil(total.toDouble() / daily).toInt() else 0
        totalWords = total
        estimatedDays = days
        // Persist the estimate so the planned day-count survives restarts.
        withContext(Dispatchers.IO) { ReviewSettingsStore(context).targetDays = days }
    }

    fun begin(ignoreDailyLimit: Boolean) {
        if (building) return
        building = true
        scope.launch {
            val built = withContext(Dispatchers.IO) {
                buildReviewSession(context, settings, ignoreDailyLimit)
            }
            building = false
            if (built.queue.isEmpty()) {
                Toast.makeText(context, R.string.review_nothing_due, Toast.LENGTH_SHORT).show()
            } else {
                onStart(built)
            }
        }
    }

    BackHandler { onClose() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_close)
                )
            }
            Text(
                text = stringResource(R.string.menu_review_name),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.size(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.review_today_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.review_today_new, newDone, settings.maxNewPerDay))
                Text(stringResource(R.string.review_today_reviews, reviewsDone))
            }
        }

        if (totalWords > 0) {
            Spacer(Modifier.size(16.dp))
            val daily = settings.maxNewPerDay
            val percentText = String.format("%.1f%%", daily * 100.0 / totalWords)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.review_plan_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.review_plan_daily, daily, totalWords, percentText))
                    Text(stringResource(R.string.review_plan_days, estimatedDays))
                }
            }
        }
        Spacer(Modifier.size(24.dp))

        if (building) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { begin(ignoreDailyLimit = false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.review_start))
            }
            Spacer(Modifier.size(8.dp))
            OutlinedButton(
                onClick = { begin(ignoreDailyLimit = true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.review_study_more))
            }
            Text(
                text = stringResource(R.string.review_study_more_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ReviewByEnter(
    settings: TestingSettings,
    session: ReviewSession,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val padding = 8.dp

    val queue = remember { mutableStateListOf<ReviewCandidate>().also { it.addAll(session.queue) } }
    var userValue by remember { mutableStateOf("") }

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

    LaunchedEffect(state.finished) {
        while (!state.finished) {
            elapsedSeconds = (System.currentTimeMillis() - state.startMillis) / 1000
            delay(1000)
        }
    }

    if (state.finished) {
        TestSummary(state.finalElapsed, state.total, accuracyPercent(state.correct, state.attempts), results.snapshot()) {
            onExit()
        }
        return
    }

    val current = queue.first()

    // Commits the final grade, advances the queue, and resets the per-card feedback.
    fun proceed(finalCorrect: Boolean) {
        scope.launch {
            if (finalCorrect) state.correct++
            results.record(current.studyWord, finalCorrect)
            val newState = withContext(Dispatchers.IO) { commitGrade(context, current, finalCorrect) }
            advanceQueue(context, queue, current, newState, state)
            reveal = false
            awaitingDecision = false
            userValue = ""
            locked = false
        }
    }

    // Answer resolved for the current kana/kanji choice (Meaning -> JP only; no-op otherwise).
    val currentAnswer = current.studyWord.withKana(showKana).answer

    BackHandler(enabled = !state.finished) { showStopDialog = true }
    if (showStopDialog) {
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; scope.launch { stopReview(context, state) } },
            onDismiss = { showStopDialog = false }
        )
    }

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
            progressLabel = stringResource(R.string.review_mastered, state.completed, state.total)
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
            // Wrong answer: let the user continue or claim the normalization was too strict.
            Row(horizontalArrangement = Arrangement.spacedBy(padding)) {
                Button(onClick = { proceed(false) }) {
                    Text(stringResource(R.string.review_continue))
                }
                OutlinedButton(onClick = { proceed(true) }) {
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
                        scope.launch {
                            delay(FEEDBACK_MS)
                            proceed(true)
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
}

@Composable
private fun ReviewByCards(
    settings: TestingSettings,
    session: ReviewSession,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pools = remember { session.pools }
    fun optionsFor(candidate: ReviewCandidate): List<StudyWord> =
        buildOptions(pools[candidate.direction].orEmpty(), candidate.studyWord)

    val queue = remember { mutableStateListOf<ReviewCandidate>().also { it.addAll(session.queue) } }
    var currentWords by remember { mutableStateOf(optionsFor(queue.first())) }

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

    LaunchedEffect(state.finished) {
        while (!state.finished) {
            elapsedSeconds = (System.currentTimeMillis() - state.startMillis) / 1000
            delay(1000)
        }
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
        scope.launch {
            val newState = withContext(Dispatchers.IO) { commitGrade(context, current, correct) }
            delay(FEEDBACK_MS)
            advanceQueue(context, queue, current, newState, state)
            reveal = false
            selectedAnswer = null
            if (!state.finished) currentWords = optionsFor(queue.first())
            locked = false
        }
    }

    BackHandler(enabled = !state.finished) { showStopDialog = true }
    if (showStopDialog) {
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; scope.launch { stopReview(context, state) } },
            onDismiss = { showStopDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
            .padding(8.dp)
    ) {
        TestStatsHeader(
            elapsedSeconds, state.completed, state.total,
            onStop = { showStopDialog = true },
            progressLabel = stringResource(R.string.review_mastered, state.completed, state.total)
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

// Grades [candidate], persists the new card state, records the answer stat, and bumps the right
// daily counter. Returns the new scheduler state. Blocking — call inside withContext(Dispatchers.IO).
private suspend fun commitGrade(
    context: Context,
    candidate: ReviewCandidate,
    correct: Boolean
): SchedulerState {
    val now = System.currentTimeMillis()
    val prev = candidate.state ?: scheduler.initialState(now)
    val newState = scheduler.review(prev, if (correct) Grade.GOOD else Grade.AGAIN, now)
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
    state: ReviewSessionState
) {
    queue.removeAt(0)
    val stillLearning = newState.phase == CardPhase.LEARNING || newState.phase == CardPhase.RELEARNING
    if (stillLearning) {
        queue.add(minOf(queue.size, REINSERT_GAP), current.copy(state = newState))
    } else {
        state.completed++
    }
    if (queue.isEmpty()) {
        val durationMillis = System.currentTimeMillis() - state.startMillis
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
private suspend fun stopReview(context: Context, state: ReviewSessionState) {
    if (state.finished) return
    val durationMillis = System.currentTimeMillis() - state.startMillis
    state.finalElapsed = durationMillis / 1000
    state.finished = true
    withContext(Dispatchers.IO) {
        recordSession(context, durationMillis, state.total, state.attempts, state.correct)
    }
}
