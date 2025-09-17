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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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

    var wordsList by remember {
        mutableStateOf<MutableList<Word>>(mutableListOf())
    }
    var currentTestingWord by remember { mutableStateOf(Word(word = "", meaning = "")) }
    var userValue by remember { mutableStateOf("") }
    var testTextColor by remember { mutableStateOf(Color.White) }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(top = 40.dp)
            .padding(padding)) {

        LaunchedEffect(Unit){
            composableScope.launch {
                wordsList = wordDao.getAllWords().toMutableList()
                wordsList.shuffle()
                currentTestingWord = wordsList.random()
            }
        }

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
            if(checkAnswer(currentTestingWord, userValue)){
                composableScope.launch {
                    Toast.makeText(context, "Good", Toast.LENGTH_SHORT).show()
                    testTextColor = Color.Green
                    delay(2000)
                    testTextColor = Color.White
                    if (wordsList.isNotEmpty())
                        currentTestingWord = wordsList.random()
                    else
                        navController.navigate(MainScreen.route)
                }
                wordsList.remove(currentTestingWord)
            }
            else {
                composableScope.launch {
                    Toast.makeText(context, "Bad", Toast.LENGTH_SHORT).show()
                    testTextColor = Color.Red
                    delay(2000)
                    testTextColor = Color.White
                    currentTestingWord = wordsList.random()
                }

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

@Composable
fun TestByCards(navController: NavController, source: WordsSource){
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    val wordDao = getDaoInstance(LocalContext.current)
    var currentTestingWord by remember { mutableStateOf(Word(word = "", meaning = ""))}
    var wordsList by remember {
        mutableStateOf<MutableList<Word>>(mutableListOf())
    }
    var wordsForUI by remember {
        mutableStateOf<MutableList<Word>>(mutableListOf())
    }
    var currentWords by remember {
        mutableStateOf<MutableList<Word>>(mutableListOf())
    }
    var testTextColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(Unit){
        composableScope.launch {
            if(source == WordsSource.MINNA){
                var number = 0
                val minna = MinnaXmlParser()
                val nihongoWords = minna.loadData(context)
                nihongoWords.forEach { wordsList.add(Word(number++, it.kana, it.translation)) }
            }
            else
                wordsList = wordDao.getAllWords().toMutableList()
            wordsList.shuffle()
            currentTestingWord = wordsList[0]
            wordsForUI = wordsList.toMutableList()
            currentWords = wordsList.take(6) as MutableList<Word>
        }
    }

    val onKanaButtonClick: (String) -> Unit = { userValue ->
        composableScope.launch {
            if(checkAnswer(currentTestingWord, userValue)){
                Toast.makeText(context, "Good", Toast.LENGTH_SHORT).show()

                testTextColor = Color.Green
                delay(2000)
                testTextColor = Color.White

                if (wordsList.isEmpty()) {
                    return@launch navController.navigate(MainScreen.route)
                }
                wordsList.remove(currentTestingWord)

                currentTestingWord = wordsList.random()
                wordsForUI.shuffle()
                currentWords = wordsForUI.take(6) as MutableList<Word>

                if(!currentWords.contains(currentTestingWord))
                    currentWords[Random.nextInt(0, 5)] = currentTestingWord
            }
            else {
                Toast.makeText(context, "Wrong. Answer: ${currentTestingWord.meaning}", Toast.LENGTH_SHORT).show()

                testTextColor = Color.Red
                delay(2000)
                testTextColor = Color.White

                currentTestingWord = wordsList.random()
                wordsForUI.shuffle()
                currentWords = wordsForUI.take(6) as MutableList<Word>

                if(!currentWords.contains(currentTestingWord))
                    currentWords[Random.nextInt(0, 5)] = currentTestingWord
            }
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

            if (wordsForUI.size != 0) {
                KanaColumn(0.5f, currentWords.take(3) as MutableList<Word>, onKanaButtonClick)
                KanaColumn(1f, currentWords.takeLast(3) as MutableList<Word>, onKanaButtonClick)
            }
        }
    }

}

@Composable
fun KanaColumn(fraction: Float, words: MutableList<Word>, onKanaButtonClick: (String) -> Unit){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(1f)
    ){
        KanaElement(word = words[0], onKanaButtonClick)
        KanaElement(word = words[1], onKanaButtonClick)
        KanaElement(word = words[2], onKanaButtonClick)
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