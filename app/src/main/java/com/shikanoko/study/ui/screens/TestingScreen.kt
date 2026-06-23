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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.shikanoko.study.ui.destination.MainScreen
import com.shikanoko.study.R
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import com.shikanoko.study.data.datasource.MinnaXmlParser
import com.shikanoko.study.data.db.Word
import com.shikanoko.study.data.db.getDaoInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TestingScreen(navController: NavController, args: MutableState<TestingSettings>){
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (args.value.testType == TestType.CARD)
            TestByCards(navController, args.value.wordsSource)
        else
        {
            TestByEnter(navController = navController)
        }
    }
}

@Composable
private fun TestByEnter(navController: NavController){
    val context = LocalContext.current
    val composableScope = rememberCoroutineScope()
    val padding = 8.dp
    val wordDao = getDaoInstance(LocalContext.current)

    val wordsList = remember { mutableStateListOf<Word>() }
    var currentTestingWord by remember { mutableStateOf(Word(word = "", meaning = "")) }
    var userValue by remember { mutableStateOf("") }
    var testTextColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(Unit){
        val loaded = withContext(Dispatchers.IO) { wordDao.getAllWords() }
        if (loaded.isEmpty()) {
            Toast.makeText(context, R.string.testing_no_words, Toast.LENGTH_SHORT).show()
            navController.popBackStack(MainScreen.route, false)
            return@LaunchedEffect
        }
        wordsList.clear()
        wordsList.addAll(loaded)
        currentTestingWord = wordsList.random()
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(top = 40.dp)
            .padding(padding)) {

        Text(text = currentTestingWord.word, fontSize = 30.sp, color = testTextColor)

        Spacer(Modifier.size(padding))

        OutlinedTextField(
            value = userValue,
            singleLine = true,
            onValueChange = { userValue = it },
            label = { Text(stringResource(id = R.string.db_meaning_name)) },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(padding))

        Button(onClick = {
            val correct = checkAnswer(currentTestingWord, userValue)
            composableScope.launch {
                if (correct) {
                    Toast.makeText(context, R.string.testing_correct, Toast.LENGTH_SHORT).show()
                    testTextColor = Color.Green
                } else {
                    Toast.makeText(context, R.string.testing_incorrect, Toast.LENGTH_SHORT).show()
                    testTextColor = Color.Red
                }
                delay(2000)
                testTextColor = Color.White
                if (correct) {
                    wordsList.remove(currentTestingWord)
                }
                if (wordsList.isEmpty()) {
                    navController.popBackStack(MainScreen.route, false)
                    return@launch
                }
                currentTestingWord = wordsList.random()
            }
            userValue = ""

        }) {
            Text(text = stringResource(id = R.string.testing_check_btn))
        }
    }
}

private fun checkAnswer(testingValue: Word, userValue: String): Boolean{
    return userValue.lowercase().trim() == testingValue.meaning.lowercase().trim() && userValue.trim() != ""
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
// drawn from the whole pool, shuffled. Returns up to [count] options (fewer if
// the pool is smaller), and always includes [correct].
private fun buildOptions(pool: List<Word>, correct: Word, count: Int = CARD_OPTION_COUNT): List<Word> {
    val distractors = pool.filter { it != correct }.shuffled().take(count - 1)
    return (distractors + correct).shuffled()
}

@Composable
fun TestByCards(navController: NavController, source: WordsSource){
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    val wordDao = getDaoInstance(LocalContext.current)
    var currentTestingWord by remember { mutableStateOf(Word(word = "", meaning = ""))}
    val wordsList = remember { mutableStateListOf<Word>() }
    val wordsForUI = remember { mutableStateListOf<Word>() }
    var currentWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var testTextColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(Unit){
        val loaded = withContext(Dispatchers.IO) {
            if (source == WordsSource.MINNA) {
                var number = 0
                val minna = MinnaXmlParser(context.resources.getXml(R.xml.nihon))
                minna.getAllWords().map { Word(number++, it.kana, it.translation) }
            } else {
                wordDao.getAllWords()
            }
        }
        if (loaded.isEmpty()) {
            Toast.makeText(context, R.string.testing_no_words, Toast.LENGTH_SHORT).show()
            navController.popBackStack(MainScreen.route, false)
            return@LaunchedEffect
        }
        wordsForUI.clear()
        wordsForUI.addAll(loaded)
        wordsList.clear()
        wordsList.addAll(loaded.shuffled())
        currentTestingWord = wordsList.first()
        currentWords = buildOptions(wordsForUI, currentTestingWord)
    }

    val onKanaButtonClick: (String) -> Unit = { userValue ->
        composableScope.launch {
            val correct = checkAnswer(currentTestingWord, userValue)
            if (correct) {
                Toast.makeText(context, R.string.testing_correct, Toast.LENGTH_SHORT).show()
                testTextColor = Color.Green
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.testing_wrong_answer, currentTestingWord.meaning),
                    Toast.LENGTH_SHORT
                ).show()
                testTextColor = Color.Red
            }
            delay(2000)
            testTextColor = Color.White

            if (correct) {
                wordsList.remove(currentTestingWord)
            }
            if (wordsList.isEmpty()) {
                navController.popBackStack(MainScreen.route, false)
                return@launch
            }
            currentTestingWord = wordsList.random()
            currentWords = buildOptions(wordsForUI, currentTestingWord)
        }
    }

    Column (horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .padding(top = 40.dp)
            .padding(8.dp)) {
        Text(text = currentTestingWord.word, fontSize = 30.sp, color = testTextColor)

        Spacer(Modifier.padding(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
        ){

            if (currentWords.isNotEmpty()) {
                KanaColumn(0.5f, currentWords.take(3), onKanaButtonClick)
                KanaColumn(1f, currentWords.drop(3), onKanaButtonClick)
            }
        }
    }

}

@Composable
fun KanaColumn(fraction: Float, words: List<Word>, onKanaButtonClick: (String) -> Unit){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(1f)
    ){
        words.forEach { word ->
            KanaElement(word = word, onKanaButtonClick)
        }
    }
}

@Composable
fun KanaElement(word: Word, onKanaButtonClick: (String) -> Unit){
    Button(
        onClick = { onKanaButtonClick(word.meaning) },
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .size(100.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(30)
    ){
        Text(word.meaning)
    }
}