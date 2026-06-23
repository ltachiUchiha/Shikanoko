package com.shikanoko.study.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.shikanoko.study.ui.destination.MainScreen
import com.shikanoko.study.R
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.formatElapsed
import com.shikanoko.study.data.loadStudyWords
import com.shikanoko.study.data.recordAnswer
import com.shikanoko.study.data.recordSession
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.ui.theme.OnSuccessContainer
import com.shikanoko.study.ui.theme.SuccessContainer
import com.shikanoko.study.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// How long the correct/incorrect highlight stays on screen before advancing.
private const val FEEDBACK_MS = 1500L

@Composable
fun TestingScreen(navController: NavController, args: MutableState<TestingSettings>){
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (args.value.testType == TestType.CARD)
            TestByCards(navController, args.value)
        else
            TestByEnter(navController, args.value)
    }
}

@Composable
private fun TestByEnter(navController: NavController, settings: TestingSettings){
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
    }

    // Live timer: ticks once a second until the test is finished.
    LaunchedEffect(finished) {
        while (!finished) {
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            delay(1000)
        }
    }

    if (finished) {
        TestSummary(finalElapsed, totalWords, accuracyPercent(correctAttempts, totalAttempts)) {
            navController.popBackStack(MainScreen.route, false)
        }
        return
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .padding(padding)) {

        TestStatsHeader(elapsedSeconds, totalWords - wordsList.size, totalWords)

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
                composableScope.launch {
                    withContext(Dispatchers.IO) { recordAnswer(context, currentTestingWord, correct) }
                    delay(FEEDBACK_MS)
                    reveal = false
                    userValue = ""
                    if (correct || !settings.retryWrongAnswers) wordsList.remove(currentTestingWord)
                    if (wordsList.isEmpty()) {
                        finishTest(
                            context, startTime, totalWords, totalAttempts, correctAttempts,
                            setElapsed = { finalElapsed = it }, setFinished = { finished = true }
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
}

private fun checkAnswer(testingValue: StudyWord, userValue: String): Boolean{
    return userValue.lowercase().trim() == testingValue.answer.lowercase().trim() && userValue.trim() != ""
}

// Records the finished session and flips the screen to the summary. Suspend so the
// DB write happens off the main thread before the timer/state settle.
private suspend fun finishTest(
    context: android.content.Context,
    startTime: Long,
    totalWords: Int,
    totalAttempts: Int,
    correctAttempts: Int,
    setElapsed: (Long) -> Unit,
    setFinished: () -> Unit
) {
    val durationMillis = System.currentTimeMillis() - startTime
    setElapsed(durationMillis / 1000)
    withContext(Dispatchers.IO) {
        recordSession(context, durationMillis, totalWords, totalAttempts, correctAttempts)
    }
    setFinished()
}

// Slim header pinned above the test: elapsed time, mastered/total count, progress bar.
@Composable
private fun TestStatsHeader(elapsedSeconds: Long, mastered: Int, total: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatElapsed(elapsedSeconds),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.testing_progress, mastered, total),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.size(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else mastered / total.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Inline correct/incorrect message used by the text-entry test (replaces the toast).
@Composable
private fun AnswerBanner(reveal: Boolean, correct: Boolean, correctAnswer: String) {
    if (!reveal) return
    if (correct) {
        Text(
            text = stringResource(R.string.testing_correct),
            color = SuccessGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    } else {
        Text(
            text = stringResource(R.string.testing_wrong_answer, correctAnswer),
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// End-of-test results: time spent, words tested, accuracy.
@Composable
private fun TestSummary(elapsedSeconds: Long, totalWords: Int, accuracy: Int, onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.summary_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.summary_time, formatElapsed(elapsedSeconds)))
        Text(stringResource(R.string.summary_words, totalWords))
        Text(stringResource(R.string.summary_accuracy, accuracy))
        Spacer(Modifier.size(24.dp))
        Button(onClick = onDone) {
            Text(stringResource(R.string.summary_done_btn))
        }
    }
}

// Shows the word being tested, with the kana reading as a subtitle when it adds information.
@Composable
private fun PromptText(word: StudyWord){
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = word.prompt, fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
        if (word.reading.isNotBlank()) {
            Text(text = word.reading, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
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

// Number of answer buttons shown in the card test (2 columns of up to 3).
private const val CARD_OPTION_COUNT = 6

// Builds the answer choices: the correct word plus distinct random distractors
// drawn from the whole pool, shuffled. Distractors never repeat the correct
// answer text. Returns up to [count] options (fewer if the pool is smaller).
private fun buildOptions(pool: List<StudyWord>, correct: StudyWord, count: Int = CARD_OPTION_COUNT): List<StudyWord> {
    val distractors = pool
        .filter { it.answer != correct.answer }
        .distinctBy { it.answer }
        .shuffled()
        .take(count - 1)
    return (distractors + correct).shuffled()
}

@Composable
fun TestByCards(navController: NavController, settings: TestingSettings){
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
    }

    // Live timer: ticks once a second until the test is finished.
    LaunchedEffect(finished) {
        while (!finished) {
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
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
            composableScope.launch {
                withContext(Dispatchers.IO) { recordAnswer(context, currentTestingWord, correct) }
                delay(FEEDBACK_MS)
                reveal = false
                selectedAnswer = null
                if (correct || !settings.retryWrongAnswers) wordsList.remove(currentTestingWord)
                if (wordsList.isEmpty()) {
                    finishTest(
                        context, startTime, totalWords, totalAttempts, correctAttempts,
                        setElapsed = { finalElapsed = it }, setFinished = { finished = true }
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
        TestSummary(finalElapsed, totalWords, accuracyPercent(correctAttempts, totalAttempts)) {
            navController.popBackStack(MainScreen.route, false)
        }
        return
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)
            .padding(8.dp)) {

        TestStatsHeader(elapsedSeconds, totalWords - wordsList.size, totalWords)

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

}

@Composable
fun KanaColumn(
    fraction: Float,
    words: List<StudyWord>,
    correctAnswer: String,
    selectedAnswer: String?,
    reveal: Boolean,
    locked: Boolean,
    onKanaButtonClick: (String) -> Unit
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(1f)
    ){
        words.forEach { word ->
            KanaElement(word, correctAnswer, selectedAnswer, reveal, locked, onKanaButtonClick)
        }
    }
}

@Composable
fun KanaElement(
    word: StudyWord,
    correctAnswer: String,
    selectedAnswer: String?,
    reveal: Boolean,
    locked: Boolean,
    onKanaButtonClick: (String) -> Unit
){
    // During the reveal window the buttons are disabled, so the highlighted cards
    // also set the matching disabled colors to stay visible.
    val isCorrect = reveal && word.answer == correctAnswer
    val isWrongPick = reveal && word.answer == selectedAnswer && word.answer != correctAnswer
    val colors = when {
        isCorrect -> ButtonDefaults.buttonColors(
            containerColor = SuccessContainer,
            contentColor = OnSuccessContainer,
            disabledContainerColor = SuccessContainer,
            disabledContentColor = OnSuccessContainer
        )
        isWrongPick -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
            disabledContentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        else -> ButtonDefaults.buttonColors()
    }
    Button(
        onClick = { onKanaButtonClick(word.answer) },
        enabled = !locked,
        colors = colors,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .size(100.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(30)
    ){
        Text(word.answer)
    }
}
