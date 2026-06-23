package com.shikanoko.study.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shikanoko.study.R
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingSettingsDialog(onDismiss: () -> Unit, onConfirm: (TestingSettings) -> Unit){
    val context = LocalContext.current

    var selectedSource by remember { mutableStateOf(WordsSource.MINNA) }
    var selectedLanguage by remember { mutableStateOf(MinnaLanguage.fromLocale(context)) }
    var checkTypeOfTest by remember { mutableStateOf(false) }
    val selectedLessons = remember { mutableStateListOf<Int>() }
    val availableLessons = remember { mutableStateListOf<Int>() }

    // Lessons are the same set across languages; load once.
    LaunchedEffect(Unit) {
        val lessons = withContext(Dispatchers.IO) {
            MinnaCsvParser.availableLessons(context, MinnaLanguage.EN)
        }
        availableLessons.clear()
        availableLessons.addAll(lessons)
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_name_popup),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Words source
                Text(stringResource(R.string.settings_words_source))
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    val buttonText = when (selectedSource) {
                        WordsSource.LOCAL -> stringResource(R.string.settings_source_local)
                        WordsSource.MINNA -> stringResource(R.string.settings_source_minna)
                    }
                    Button(onClick = { expanded = !expanded }) {
                        Text(buttonText)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_source_minna)) },
                            onClick = { selectedSource = WordsSource.MINNA; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_source_local)) },
                            onClick = { selectedSource = WordsSource.LOCAL; expanded = false }
                        )
                    }
                }

                if (selectedSource == WordsSource.MINNA) {
                    // Translation language
                    Text(stringResource(R.string.settings_language))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLanguage == MinnaLanguage.EN,
                            onClick = { selectedLanguage = MinnaLanguage.EN },
                            label = { Text(stringResource(R.string.settings_lang_en)) }
                        )
                        FilterChip(
                            selected = selectedLanguage == MinnaLanguage.RU,
                            onClick = { selectedLanguage = MinnaLanguage.RU },
                            label = { Text(stringResource(R.string.settings_lang_ru)) }
                        )
                    }

                    // Lessons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_lessons))
                        Row {
                            TextButton(onClick = {
                                selectedLessons.clear()
                                selectedLessons.addAll(availableLessons)
                            }) { Text(stringResource(R.string.settings_select_all)) }
                            TextButton(onClick = { selectedLessons.clear() }) {
                                Text(stringResource(R.string.settings_clear))
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(items = availableLessons) { lesson ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedLessons.contains(lesson)) selectedLessons.remove(lesson)
                                        else selectedLessons.add(lesson)
                                    }
                            ) {
                                Checkbox(
                                    checked = selectedLessons.contains(lesson),
                                    onCheckedChange = null
                                )
                                Text(
                                    text = stringResource(R.string.settings_lesson_n, lesson),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Test type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Checkbox(checked = checkTypeOfTest, onCheckedChange = { checkTypeOfTest = it })
                    Text(
                        text = stringResource(R.string.settings_test_by_enter),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(stringResource(R.string.dialog_close))
                    }
                    TextButton(onClick = {
                        val testType = if (checkTypeOfTest) TestType.TEXT else TestType.CARD
                        onConfirm(
                            TestingSettings(
                                wordsSource = selectedSource,
                                testType = testType,
                                language = selectedLanguage,
                                lessons = selectedLessons.toSet()
                            )
                        )
                    }) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }
}
