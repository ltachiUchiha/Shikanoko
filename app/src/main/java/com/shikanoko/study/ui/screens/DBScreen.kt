package com.shikanoko.study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shikanoko.study.R
import com.shikanoko.study.data.db.Word
import com.shikanoko.study.data.db.getDaoInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DBScreen () {
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        val composableScope = rememberCoroutineScope()
        val wordDao = getDaoInstance(LocalContext.current)
        val words = remember { mutableStateListOf<Word>() }
        val padding = 8.dp
        Column (
            Modifier
                .padding(top = 40.dp)
                .padding(padding)){
            var word by rememberSaveable { mutableStateOf("") }
            var meaning by rememberSaveable { mutableStateOf("") }

            OutlinedTextField(
                value = word,
                singleLine = true,
                onValueChange = { word = it },
                label = { Text(stringResource(id = R.string.db_word_name)) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(padding))
            OutlinedTextField(
                value = meaning,
                singleLine = true,
                onValueChange = { meaning = it },
                label = { Text(stringResource(id = R.string.db_meaning_name)) },
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
                    words.clear()
                    words.addAll(wordDao.getAllWords())
                }
            }) {
                Text(stringResource(id = R.string.db_add_button))
            }

            Button(onClick = {
                composableScope.launch {
                    wordDao.deleteAllWords()
                    words.clear()
                }
            }) {
                Text("Delete all words")
            }
            Spacer(Modifier.size(padding))
            LaunchedEffect(Unit) {
                words.addAll(wordDao.getAllWords())
            }

            LazyColumn (modifier = Modifier.fillMaxHeight()){

                items(items = words, key = {it.id}) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { state ->
                            if(state == SwipeToDismissBoxValue.EndToStart){
                                words.remove(it)
                                composableScope.launch {
                                    wordDao.deleteWord(it)
                                    words.clear()
                                    words.addAll(wordDao.getAllWords())
                                }
                                true
                            }
                            else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = true,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error)
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
                        content = {
                            OutlinedCard(shape = RectangleShape) {
                                ListItem(headlineContent = { Text(text = it.word) },
                                    supportingContent = { Text(text = it.meaning) })
                            }
                        })

                }
            }
        }
    }
}