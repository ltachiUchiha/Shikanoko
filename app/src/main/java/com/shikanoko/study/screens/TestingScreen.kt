package com.shikanoko.study.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation.compose.rememberNavController
import com.shikanoko.study.R
import com.shikanoko.study.Word
import com.shikanoko.study.getDaoInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TestingScreen(navController: NavController){
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        val context = LocalContext.current
        val composableScope = rememberCoroutineScope()
        val padding = 8.dp
        val wordDao = getDaoInstance(LocalContext.current)
        var wordsList by remember {
            mutableStateOf<MutableList<Word>>(mutableListOf())
        }

        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(top = 40.dp)
                .padding(padding)) {

            var testingValue by remember { mutableStateOf(Word(word = "", meaning = "")) }
            var userValue by remember { mutableStateOf("") }
            var testTextColor by remember { mutableStateOf(Color.White) }

            LaunchedEffect(Unit){
                composableScope.launch {
                    wordsList = wordDao.getAllWords().toMutableList()
                    wordsList.shuffle()
                    testingValue = wordsList.random()
                }
            }

            Text(text = testingValue.word, fontSize = 30.sp, color = testTextColor)

            Spacer(Modifier.size(padding))

            OutlinedTextField(
                value = userValue,
                singleLine = true,
                onValueChange = { userValue = it },
                label = { Text(stringResource(id = R.string.db_meaning_name)) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(padding))

            Button(onClick = {
                if(checkAnswer(testingValue, userValue)){
                    composableScope.launch {
                        Toast.makeText(context, "Good", Toast.LENGTH_SHORT).show()
                        testTextColor = Color.Green
                        delay(2000)
                        testTextColor = Color.White
                        if (wordsList.isNotEmpty())
                            testingValue = wordsList.random()
                        else
                            navController.navigate(com.shikanoko.study.MainScreen.route)
                    }
                    wordsList.remove(testingValue)
                }
                else {
                    composableScope.launch {
                        Toast.makeText(context, "Bad", Toast.LENGTH_SHORT).show()
                        testTextColor = Color.Red
                        delay(2000)
                        testTextColor = Color.White
                        testingValue = wordsList.random()
                    }

                }
                userValue = ""

            }) {
                Text(text = stringResource(id = R.string.testing_check_btn))
            }
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
fun TestCard(){
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(1f)
    ){
        KanaColumn(0.5f)
        KanaColumn(1f)
    }
}

@Composable
fun KanaColumn(fraction: Float){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(0.5f)
    ){
        KanaElement(text = "Тест1", 1)
        KanaElement(text = "Тест2", 2)
        KanaElement(text = "Тест3", 3)
    }
}

@Composable
fun KanaElement(text: String, number: Int){
    Button(
        onClick = { /*TODO*/ },
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .size(100.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(30)
    ){
        Text(text)
    }
}