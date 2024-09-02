package com.shikanoko.study.screens

import android.content.Context
import android.graphics.drawable.Icon
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.shikanoko.study.NokoDao
import com.shikanoko.study.NokoDatabase
import com.shikanoko.study.Word
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DBScreen () {
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        val composableScope = rememberCoroutineScope()
        val db = Room.databaseBuilder(LocalContext.current, NokoDatabase::class.java, "noko-db")
            .build()
        val wordDao = db.nokoDao()
        var words by remember {
            mutableStateOf<List<Word>>(emptyList())
        }
        val padding = 8.dp
        Column (Modifier.padding(top = 40.dp).padding(padding)){
            var word by rememberSaveable { mutableStateOf("") }
            var meaning by rememberSaveable { mutableStateOf("") }

            OutlinedTextField(
                value = word,
                singleLine = true,
                onValueChange = { word = it },
                label = { Text("Слово") },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(padding))
            OutlinedTextField(
                value = meaning,
                singleLine = true,
                onValueChange = { meaning = it },
                label = { Text("Значение") },
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.size(padding))

            Button(onClick = {
                word = word.trim()
                meaning = meaning.trim()
                if (word == "" || meaning == "") {
                    return@Button
                }
                composableScope.launch {
                wordDao.insertWord(Word(word = word, meaning = meaning))
                word = ""
                meaning = ""
                words = wordDao.getAllWords()
                }
            }) {
                Text("Добавить слово")
            }
            Spacer(Modifier.size(padding))
            LaunchedEffect(Unit) {
                //wordDao.insertWord(Word(meaning = "Спасибо", word = "ありがとう"))
                words = wordDao.getAllWords()
            }
            LazyColumn{
                itemsIndexed(words) { _, it ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = true,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error) // TODO: Change color
                                    .padding(12.dp, 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            )
                            {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "delete"
                                )
                            }
                        },
                    ) {
                        OutlinedCard(shape = RectangleShape) {
                            ListItem(headlineContent = { Text(text = it.word) },
                                supportingContent = { Text(text = it.meaning) })
                        }
                    }

                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        LaunchedEffect(Unit) {
                            wordDao.deleteWord(it)
                            words = wordDao.getAllWords()
                        }
                    }
                }
            }
        }
    }
}