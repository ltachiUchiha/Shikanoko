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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shikanoko.study.R
import com.shikanoko.study.data.ReviewSettingsStore
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.TestType
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Lesson numbers up to this value belong to "Minna I"; the rest to "Minna II".
private const val MINNA_I_LAST_LESSON = 25

// Bounds and step for the review-only "new words per day" stepper.
private const val MIN_NEW_PER_DAY = 5
private const val MAX_NEW_PER_DAY = 50
private const val NEW_PER_DAY_STEP = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (TestingSettings) -> Unit,
    // The Review flow drills separate recall directions; the practice flow does not, so the
    // direction picker (and the practice-only "repeat wrong answers" toggle) are swapped per mode.
    showDirections: Boolean = false
) {
    val context = LocalContext.current
    // Review settings persist across launches; the practice flow doesn't use this store.
    val reviewStore = remember { ReviewSettingsStore(context) }

    var selectedSource by remember { mutableStateOf(WordsSource.MINNA) }
    var selectedTestType by remember { mutableStateOf(TestType.CARD) }
    var selectedLanguage by remember { mutableStateOf(MinnaLanguage.fromLocale(context)) }
    var retryWrongAnswers by remember { mutableStateOf(true) }
    val selectedLessons = remember { mutableStateListOf<Int>() }
    val availableLessons = remember { mutableStateListOf<Int>() }
    val selectedDirections = remember {
        mutableStateListOf(Direction.JP_TO_MEANING, Direction.MEANING_TO_JP)
    }
    var showKana by remember { mutableStateOf(false) }
    var maxNewPerDay by remember { mutableStateOf(TestingSettings().maxNewPerDay) }
    // Review only: highest lesson the user knows; review covers lessons 1..upToLesson.
    var upToLesson by remember { mutableStateOf(0) }

    // Lessons are the same set across languages; load once. In review mode also seed the persisted
    // up-to-lesson and daily-pace values (defaulting "up to" to the last lesson — i.e. everything).
    LaunchedEffect(Unit) {
        val (lessons, savedUpTo, savedPerDay) = withContext(Dispatchers.IO) {
            Triple(
                MinnaCsvParser.availableLessons(context, MinnaLanguage.EN),
                reviewStore.upToLesson,
                reviewStore.maxNewPerDay
            )
        }
        availableLessons.clear()
        availableLessons.addAll(lessons)
        if (showDirections) {
            maxNewPerDay = savedPerDay
            upToLesson = if (savedUpTo > 0) savedUpTo else (lessons.lastOrNull() ?: 0)
        }
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
                                if (showDirections) {
                                    // Remember the review scope + daily pace for next time.
                                    reviewStore.upToLesson = upToLesson
                                    reviewStore.maxNewPerDay = maxNewPerDay
                                }
                                onConfirm(
                                    TestingSettings(
                                        wordsSource = selectedSource,
                                        testType = selectedTestType,
                                        language = selectedLanguage,
                                        lessons = selectedLessons.toSet(),
                                        retryWrongAnswers = retryWrongAnswers,
                                        directions = selectedDirections.toSet(),
                                        showKana = showKana,
                                        upToLesson = upToLesson,
                                        maxNewPerDay = maxNewPerDay
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

                    if (showDirections) {
                        // Recall directions (Review only): each drills the word independently.
                        DirectionSection(selectedDirections)
                        // Show hiragana instead of kanji (Review only): applies to Meaning -> JP.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_show_kana),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_show_kana_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showKana,
                                onCheckedChange = { showKana = it }
                            )
                        }
                        // New words per day (Review only): caps how many brand-new cards Review
                        // introduces in a normal session.
                        NewPerDaySection(maxNewPerDay) { maxNewPerDay = it }
                    } else {
                        // Repeat wrong answers (practice only)
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

                        if (showDirections) {
                            // Review: a single "up to lesson N" scope instead of multi-select.
                            UpToLessonSection(upToLesson, availableLessons.lastOrNull() ?: 0) {
                                upToLesson = it
                            }
                        } else {
                            // Practice: multi-select lesson chips.
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
}

// Multi-select recall directions for the Review flow. Each selected direction becomes its own
// independently-scheduled card per word.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSection(selected: SnapshotStateList<Direction>) {
    SettingSection(stringResource(R.string.settings_directions)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DirectionChip(Direction.JP_TO_MEANING, R.string.settings_dir_jp_meaning, selected)
            DirectionChip(Direction.MEANING_TO_JP, R.string.settings_dir_meaning_jp, selected)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionChip(direction: Direction, labelRes: Int, selected: SnapshotStateList<Direction>) {
    FilterChip(
        selected = direction in selected,
        onClick = {
            if (direction in selected) selected.remove(direction) else selected.add(direction)
        },
        label = { Text(stringResource(labelRes)) }
    )
}

// Review-only: the single "I know words up to lesson N" input. Review draws from lessons 1..N.
// [maxLesson] is the highest available lesson; typed values are coerced to 1..maxLesson.
@Composable
private fun UpToLessonSection(value: Int, maxLesson: Int, onChange: (Int) -> Unit) {
    SettingSection(stringResource(R.string.settings_up_to_lesson)) {
        Text(
            text = stringResource(R.string.settings_up_to_lesson_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = if (value > 0) value.toString() else "",
            onValueChange = { text ->
                val n = text.filter(Char::isDigit).take(3).toIntOrNull() ?: 0
                onChange(if (maxLesson > 0) n.coerceIn(0, maxLesson) else n)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = {
                if (maxLesson > 0) Text(stringResource(R.string.settings_up_to_lesson_of, maxLesson))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Review-only stepper for how many new words to introduce per day. Adjusts by [NEW_PER_DAY_STEP]
// within [MIN_NEW_PER_DAY]..[MAX_NEW_PER_DAY].
@Composable
private fun NewPerDaySection(value: Int, onChange: (Int) -> Unit) {
    SettingSection(stringResource(R.string.settings_new_per_day)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.settings_new_per_day_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { onChange((value - NEW_PER_DAY_STEP).coerceAtLeast(MIN_NEW_PER_DAY)) },
                enabled = value > MIN_NEW_PER_DAY
            ) { Text("−") }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            OutlinedButton(
                onClick = { onChange((value + NEW_PER_DAY_STEP).coerceAtMost(MAX_NEW_PER_DAY)) },
                enabled = value < MAX_NEW_PER_DAY
            ) { Text("+") }
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
