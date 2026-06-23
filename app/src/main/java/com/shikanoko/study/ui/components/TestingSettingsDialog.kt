package com.shikanoko.study.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shikanoko.study.R
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Lesson numbers up to this value belong to "Minna I"; the rest to "Minna II".
private const val MINNA_I_LAST_LESSON = 25

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingSettingsDialog(onDismiss: () -> Unit, onConfirm: (TestingSettings) -> Unit) {
    val context = LocalContext.current

    var selectedSource by remember { mutableStateOf(WordsSource.MINNA) }
    var selectedTestType by remember { mutableStateOf(TestType.CARD) }
    var selectedLanguage by remember { mutableStateOf(MinnaLanguage.fromLocale(context)) }
    var retryWrongAnswers by remember { mutableStateOf(true) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings_name_popup)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dialog_close)
                                )
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                onConfirm(
                                    TestingSettings(
                                        wordsSource = selectedSource,
                                        testType = selectedTestType,
                                        language = selectedLanguage,
                                        lessons = selectedLessons.toSet(),
                                        retryWrongAnswers = retryWrongAnswers
                                    )
                                )
                            }) {
                                Text(stringResource(R.string.settings_start))
                            }
                        },
                        // The dialog window already insets content below the status bar.
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Words source
                    SettingSection(stringResource(R.string.settings_words_source)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedSource == WordsSource.MINNA,
                                onClick = { selectedSource = WordsSource.MINNA },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text(stringResource(R.string.settings_source_minna)) }
                            SegmentedButton(
                                selected = selectedSource == WordsSource.LOCAL,
                                onClick = { selectedSource = WordsSource.LOCAL },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text(stringResource(R.string.settings_source_local)) }
                        }
                    }

                    // Test type
                    SettingSection(stringResource(R.string.settings_test_type)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedTestType == TestType.CARD,
                                onClick = { selectedTestType = TestType.CARD },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text(stringResource(R.string.settings_test_cards)) }
                            SegmentedButton(
                                selected = selectedTestType == TestType.TEXT,
                                onClick = { selectedTestType = TestType.TEXT },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text(stringResource(R.string.settings_test_text)) }
                        }
                    }

                    // Repeat wrong answers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_retry_wrong),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.settings_retry_wrong_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = retryWrongAnswers,
                            onCheckedChange = { retryWrongAnswers = it }
                        )
                    }

                    if (selectedSource == WordsSource.MINNA) {
                        // Translation language
                        SettingSection(stringResource(R.string.settings_language)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = selectedLanguage == MinnaLanguage.EN,
                                    onClick = { selectedLanguage = MinnaLanguage.EN },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) { Text(stringResource(R.string.settings_lang_en)) }
                                SegmentedButton(
                                    selected = selectedLanguage == MinnaLanguage.RU,
                                    onClick = { selectedLanguage = MinnaLanguage.RU },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) { Text(stringResource(R.string.settings_lang_ru)) }
                            }
                        }

                        // Lessons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_lessons),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (selectedLessons.isEmpty())
                                    stringResource(R.string.settings_lessons_all)
                                else
                                    stringResource(R.string.settings_lessons_selected, selectedLessons.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LessonGroup(
                            groupName = stringResource(R.string.settings_group_minna1),
                            lessons = availableLessons.filter { it <= MINNA_I_LAST_LESSON },
                            selectedLessons = selectedLessons
                        )
                        LessonGroup(
                            groupName = stringResource(R.string.settings_group_minna2),
                            lessons = availableLessons.filter { it > MINNA_I_LAST_LESSON },
                            selectedLessons = selectedLessons
                        )
                    }
                }
            }
        }
    }
}

// A labelled block: small section title above its control.
@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

// One lesson group (e.g. "Minna I"): a header with its number range and per-group
// All/Clear actions, then a wrapping grid of selectable lesson-number chips.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonGroup(
    groupName: String,
    lessons: List<Int>,
    selectedLessons: SnapshotStateList<Int>
) {
    if (lessons.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = groupName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_lessons_range, lessons.first(), lessons.last()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                lessons.forEach { if (it !in selectedLessons) selectedLessons.add(it) }
            }) { Text(stringResource(R.string.settings_select_all)) }
            TextButton(onClick = {
                selectedLessons.removeAll(lessons)
            }) { Text(stringResource(R.string.settings_clear)) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            lessons.forEach { lesson ->
                FilterChip(
                    selected = lesson in selectedLessons,
                    onClick = {
                        if (lesson in selectedLessons) selectedLessons.remove(lesson)
                        else selectedLessons.add(lesson)
                    },
                    label = { Text(lesson.toString()) }
                )
            }
        }
    }
}
