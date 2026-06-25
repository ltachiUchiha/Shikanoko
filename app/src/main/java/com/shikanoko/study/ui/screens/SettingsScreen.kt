package com.shikanoko.study.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shikanoko.study.R
import com.shikanoko.study.data.ReviewSettingsStore
import com.shikanoko.study.data.countScopeWords
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

// Bounds and step for the review "new words per day" stepper.
private const val MIN_NEW_PER_DAY = 5
private const val MAX_NEW_PER_DAY = 50
private const val NEW_PER_DAY_STEP = 5

// App settings, persisted in [ReviewSettingsStore]. Currently holds the Review configuration (daily
// pace + lesson scope) and the resulting review plan; other settings can be added here later.
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { ReviewSettingsStore(context) }

    var maxNewPerDay by remember { mutableStateOf(0) }
    var upToLesson by remember { mutableStateOf(0) }
    var maxLesson by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    // Total words in the chosen scope (Minna lessons 1..upToLesson) — drives the live plan.
    var totalWords by remember { mutableStateOf(0) }

    // Seed the fields from the persisted store; default "up to lesson" to the last lesson.
    LaunchedEffect(Unit) {
        val (lessons, savedUpTo, savedPerDay) = withContext(Dispatchers.IO) {
            Triple(
                MinnaCsvParser.availableLessons(context, MinnaLanguage.EN),
                store.upToLesson,
                store.maxNewPerDay
            )
        }
        maxLesson = lessons.lastOrNull() ?: 0
        upToLesson = if (savedUpTo > 0) savedUpTo else maxLesson
        maxNewPerDay = savedPerDay
        loaded = true
    }

    // Recount the scope whenever the lesson cap changes (word count is language-independent here).
    LaunchedEffect(upToLesson, loaded) {
        if (!loaded) return@LaunchedEffect
        totalWords = withContext(Dispatchers.IO) {
            countScopeWords(
                context,
                TestingSettings(wordsSource = WordsSource.MINNA, upToLesson = upToLesson)
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.menu_settings_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.menu_review_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            NewPerDaySection(maxNewPerDay) { maxNewPerDay = it }
            UpToLessonSection(upToLesson, maxLesson) { upToLesson = it }

            if (totalWords > 0) {
                val daily = maxNewPerDay.coerceAtLeast(1)
                val days = ceil(totalWords.toDouble() / daily).toInt()
                val percentText = String.format("%.1f%%", daily * 100.0 / totalWords)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.review_plan_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.review_plan_daily, maxNewPerDay, totalWords, percentText))
                        Text(stringResource(R.string.review_plan_days, days))
                    }
                }
            }

            Spacer(Modifier.size(24.dp))
            Button(
                onClick = {
                    store.upToLesson = upToLesson
                    store.maxNewPerDay = maxNewPerDay
                    val daily = maxNewPerDay.coerceAtLeast(1)
                    store.targetDays =
                        if (totalWords > 0) ceil(totalWords.toDouble() / daily).toInt() else 0
                    Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_save))
            }
        }
    }
}

// Stepper for how many new words Review introduces per day, by [NEW_PER_DAY_STEP] within bounds.
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

// The single "I know words up to lesson N" input. Review draws from lessons 1..N. [maxLesson] is the
// highest available lesson; typed values are coerced to 1..maxLesson.
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
