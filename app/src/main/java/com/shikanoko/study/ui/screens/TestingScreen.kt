package com.shikanoko.study.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shikanoko.study.ui.destination.MainScreen
import com.shikanoko.study.R
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.loadStudyWords
import com.shikanoko.study.data.recordAnswer
import com.shikanoko.study.data.recordSession
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
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
import com.shikanoko.study.ui.components.checkAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TestingScreen(navController: NavController, args: MutableState<TestingSettings>, session: TestSession){
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (args.value.testType == TestType.CARD)
            TestByCards(navController, args.value, session)
        else
            TestByEnter(navController, args.value, session)
    }
}

@Composable
private fun TestByEnter(navController: NavController, settings: TestingSettings, session: TestSession){
    val context = LocalContext.current
    val composableScope = rememberCoroutineScope()
    val padding = 8.dp

    val wordsList = remember { mutableStateListOf<StudyWord>() }
    var currentTestingWord by remember { mutableStateOf(StudyWord(prompt = "", answer = "")) }
    var userValue by remember { mutableStateOf("") }

    // Session metrics shared by both test modes.
    val startTime = remember { System.currentTimeMillis() }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var totalWords by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(0) }
    var correctAttempts by remember { mutableStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var finalElapsed by remember { mutableStateOf(0L) }
    var showStopDialog by remember { mutableStateOf(false) }
    // Per-word outcomes for the end-of-test breakdown.
    val results = remember { SessionResults() }

    // Feedback state for the current word.
    var reveal by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf(false) }

    LaunchedEffect(Unit){
        val loaded = withContext(Dispatchers.IO) { loadStudyWords(context, settings) }
        if (loaded.isEmpty()) {
            Toast.makeText(context, R.string.testing_no_words, Toast.LENGTH_SHORT).show()
            navController.popBackStack(MainScreen.route, false)
            return@LaunchedEffect
        }
        wordsList.clear()
        wordsList.addAll(loaded)
        totalWords = loaded.size
        currentTestingWord = wordsList.random()
        // Mark the session live and register how to persist a partial run if the user leaves via the
        // drawer. Idempotent: skipped once the test has finished (guards against a double record).
        session.begin()
        session.recorder = recorder@{
            if (finished) return@recorder
            val durationMillis = System.currentTimeMillis() - startTime - session.clock.pausedMillis()
            withContext(Dispatchers.IO) {
                recordSession(context, durationMillis, totalWords, totalAttempts, correctAttempts)
            }
        }
    }
    // Leaving the screen ends the session (clears active/recorder so the drawer stops intercepting).
    DisposableEffect(Unit) { onDispose { session.end() } }

    // Live timer: ticks once a second until the test is finished; paused time is subtracted so the
    // displayed value freezes while paused.
    LaunchedEffect(finished) {
        while (!finished) {
            elapsedSeconds = (System.currentTimeMillis() - startTime - session.clock.pausedMillis()) / 1000
            delay(1000)
        }
    }

    if (finished) {
        TestSummary(finalElapsed, totalWords, accuracyPercent(correctAttempts, totalAttempts), results.snapshot()) {
            navController.popBackStack(MainScreen.route, false)
        }
        return
    }

    // Ending early (X button or system back) saves the partial session and shows its summary.
    fun stopEarly() {
        composableScope.launch {
            finishTest(
                context, startTime, session.clock.pausedMillis(), totalWords, totalAttempts, correctAttempts,
                setElapsed = { finalElapsed = it }, setFinished = { finished = true; session.end() }
            )
        }
    }
    BackHandler(enabled = !finished) { showStopDialog = true }
    if (showStopDialog) {
        // Freeze the timer while the confirmation is shown, mirroring the drawer exit-warning.
        DisposableEffect(Unit) {
            session.pushDialogPause()
            onDispose { session.popDialogPause() }
        }
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; stopEarly() },
            onDismiss = { showStopDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .padding(padding)) {

        TestStatsHeader(
            elapsedSeconds, totalWords - wordsList.size, totalWords,
            onStop = { showStopDialog = true },
            paused = session.userPaused,
            pauseEnabled = !locked,
            onTogglePause = { session.toggleUserPause() }
        )

        Spacer(Modifier.size(padding))

        PromptText(currentTestingWord)

        AnswerBanner(reveal, lastCorrect, currentTestingWord.answer)

        Spacer(Modifier.size(padding))

        OutlinedTextField(
            value = userValue,
            singleLine = true,
            enabled = !locked,
            onValueChange = { userValue = it },
            label = { Text(stringResource(id = R.string.db_meaning_name)) },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(padding))

        Button(
            enabled = !locked,
            onClick = {
                if (locked) return@Button
                val correct = checkAnswer(currentTestingWord, userValue)
                locked = true
                reveal = true
                lastCorrect = correct
                totalAttempts++
                if (correct) correctAttempts++
                results.record(currentTestingWord, correct)
                composableScope.launch {
                    withContext(Dispatchers.IO) { recordAnswer(context, currentTestingWord, correct) }
                    delay(FEEDBACK_MS)
                    reveal = false
                    userValue = ""
                    if (correct || !settings.retryWrongAnswers) wordsList.remove(currentTestingWord)
                    if (wordsList.isEmpty()) {
                        finishTest(
                            context, startTime, session.clock.pausedMillis(), totalWords, totalAttempts, correctAttempts,
                            setElapsed = { finalElapsed = it }, setFinished = { finished = true; session.end() }
                        )
                        return@launch
                    }
                    currentTestingWord = wordsList.random()
                    locked = false
                }
            }) {
            Text(text = stringResource(id = R.string.testing_check_btn))
        }
    }
        if (session.userPaused) {
            PauseOverlay(onResume = { session.toggleUserPause() })
        }
    }
}

// Records the finished session and flips the screen to the summary. Suspend so the
// DB write happens off the main thread before the timer/state settle.
private suspend fun finishTest(
    context: android.content.Context,
    startTime: Long,
    pausedMillis: Long,
    totalWords: Int,
    totalAttempts: Int,
    correctAttempts: Int,
    setElapsed: (Long) -> Unit,
    setFinished: () -> Unit
) {
    val durationMillis = System.currentTimeMillis() - startTime - pausedMillis
    setElapsed(durationMillis / 1000)
    withContext(Dispatchers.IO) {
        recordSession(context, durationMillis, totalWords, totalAttempts, correctAttempts)
    }
    setFinished()
}

@Composable
fun KanjiCard(){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .padding(top = 34.dp)
            .fillMaxWidth()
    ){
        Image(painter = painterResource(id = R.drawable.shika), contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(start = 12.dp)
        )
        Column (
            modifier = Modifier
                .padding(start = 12.dp)
        ) {
            Text("Noko Shikanoko", fontWeight = FontWeight.Bold)
        }

    }
}

@Composable
fun TestByCards(navController: NavController, settings: TestingSettings, session: TestSession){
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentTestingWord by remember { mutableStateOf(StudyWord(prompt = "", answer = "")) }
    val wordsList = remember { mutableStateListOf<StudyWord>() }
    val wordsForUI = remember { mutableStateListOf<StudyWord>() }
    var currentWords by remember { mutableStateOf<List<StudyWord>>(emptyList()) }

    // Session metrics shared by both test modes.
    val startTime = remember { System.currentTimeMillis() }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var totalWords by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(0) }
    var correctAttempts by remember { mutableStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var finalElapsed by remember { mutableStateOf(0L) }
    var showStopDialog by remember { mutableStateOf(false) }
    // Per-word outcomes for the end-of-test breakdown.
    val results = remember { SessionResults() }

    // Feedback state: which card was tapped, and whether to show the highlight.
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var reveal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit){
        val loaded = withContext(Dispatchers.IO) { loadStudyWords(context, settings) }
        if (loaded.isEmpty()) {
            Toast.makeText(context, R.string.testing_no_words, Toast.LENGTH_SHORT).show()
            navController.popBackStack(MainScreen.route, false)
            return@LaunchedEffect
        }
        wordsForUI.clear()
        wordsForUI.addAll(loaded)
        wordsList.clear()
        wordsList.addAll(loaded.shuffled())
        totalWords = loaded.size
        currentTestingWord = wordsList.first()
        currentWords = buildOptions(wordsForUI, currentTestingWord)
        // Mark the session live and register how to persist a partial run if the user leaves via the
        // drawer. Idempotent: skipped once the test has finished (guards against a double record).
        session.begin()
        session.recorder = recorder@{
            if (finished) return@recorder
            val durationMillis = System.currentTimeMillis() - startTime - session.clock.pausedMillis()
            withContext(Dispatchers.IO) {
                recordSession(context, durationMillis, totalWords, totalAttempts, correctAttempts)
            }
        }
    }
    // Leaving the screen ends the session (clears active/recorder so the drawer stops intercepting).
    DisposableEffect(Unit) { onDispose { session.end() } }

    // Live timer: ticks once a second until the test is finished; paused time is subtracted so the
    // displayed value freezes while paused.
    LaunchedEffect(finished) {
        while (!finished) {
            elapsedSeconds = (System.currentTimeMillis() - startTime - session.clock.pausedMillis()) / 1000
            delay(1000)
        }
    }

    // One effective press per word: ignore taps while a check is in progress.
    val onKanaButtonClick: (String) -> Unit = { userValue ->
        if (!locked) {
            val correct = checkAnswer(currentTestingWord, userValue)
            locked = true
            reveal = true
            selectedAnswer = userValue
            totalAttempts++
            if (correct) correctAttempts++
            results.record(currentTestingWord, correct)
            composableScope.launch {
                withContext(Dispatchers.IO) { recordAnswer(context, currentTestingWord, correct) }
                delay(FEEDBACK_MS)
                reveal = false
                selectedAnswer = null
                if (correct || !settings.retryWrongAnswers) wordsList.remove(currentTestingWord)
                if (wordsList.isEmpty()) {
                    finishTest(
                        context, startTime, session.clock.pausedMillis(), totalWords, totalAttempts, correctAttempts,
                        setElapsed = { finalElapsed = it }, setFinished = { finished = true; session.end() }
                    )
                    return@launch
                }
                currentTestingWord = wordsList.random()
                currentWords = buildOptions(wordsForUI, currentTestingWord)
                locked = false
            }
        }
    }

    if (finished) {
        TestSummary(finalElapsed, totalWords, accuracyPercent(correctAttempts, totalAttempts), results.snapshot()) {
            navController.popBackStack(MainScreen.route, false)
        }
        return
    }

    // Ending early (X button or system back) saves the partial session and shows its summary.
    fun stopEarly() {
        composableScope.launch {
            finishTest(
                context, startTime, session.clock.pausedMillis(), totalWords, totalAttempts, correctAttempts,
                setElapsed = { finalElapsed = it }, setFinished = { finished = true; session.end() }
            )
        }
    }
    BackHandler(enabled = !finished) { showStopDialog = true }
    if (showStopDialog) {
        // Freeze the timer while the confirmation is shown, mirroring the drawer exit-warning.
        DisposableEffect(Unit) {
            session.pushDialogPause()
            onDispose { session.popDialogPause() }
        }
        StopConfirmDialog(
            onConfirm = { showStopDialog = false; stopEarly() },
            onDismiss = { showStopDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
            .padding(8.dp)) {

        TestStatsHeader(
            elapsedSeconds, totalWords - wordsList.size, totalWords,
            onStop = { showStopDialog = true },
            paused = session.userPaused,
            pauseEnabled = !locked,
            onTogglePause = { session.toggleUserPause() }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PromptText(currentTestingWord)

            Spacer(Modifier.padding(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
            ){
                if (currentWords.isNotEmpty()) {
                    KanaColumn(0.5f, currentWords.take(3), currentTestingWord.answer, selectedAnswer, reveal, locked, onKanaButtonClick)
                    KanaColumn(1f, currentWords.drop(3), currentTestingWord.answer, selectedAnswer, reveal, locked, onKanaButtonClick)
                }
            }
        }
    }
        if (session.userPaused) {
            PauseOverlay(onResume = { session.toggleUserPause() })
        }
    }

}
