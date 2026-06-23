package com.shikanoko.study.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shikanoko.study.R
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.MinnaWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Browse-all screen: every "Minna no Nihongo" word grouped by lesson, with an EN/RU toggle.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinnaScreen() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(MinnaLanguage.fromLocale(context)) }
    val words = remember { mutableStateListOf<MinnaWord>() }

    LaunchedEffect(language) {
        val loaded = withContext(Dispatchers.IO) { MinnaCsvParser.loadWords(context, language) }
        words.clear()
        words.addAll(loaded)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(top = 40.dp)
                .padding(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == MinnaLanguage.EN,
                    onClick = { language = MinnaLanguage.EN },
                    label = { Text(stringResource(R.string.settings_lang_en)) }
                )
                FilterChip(
                    selected = language == MinnaLanguage.RU,
                    onClick = { language = MinnaLanguage.RU },
                    label = { Text(stringResource(R.string.settings_lang_ru)) }
                )
            }

            val grouped = words.groupBy { it.lesson }
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                grouped.forEach { (lesson, lessonWords) ->
                    item(key = "lesson-$lesson") {
                        Text(
                            text = stringResource(R.string.settings_lesson_n, lesson),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                        )
                        HorizontalDivider()
                    }
                    items(items = lessonWords) { word ->
                        ListItem(
                            overlineContent = {
                                if (word.kana.isNotBlank() && word.kana != word.kanji) {
                                    Text(word.kana)
                                }
                            },
                            headlineContent = { Text(word.kanji) },
                            supportingContent = { Text(word.translation) }
                        )
                    }
                }
            }
        }
    }
}
