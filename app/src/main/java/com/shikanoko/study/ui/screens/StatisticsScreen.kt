package com.shikanoko.study.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shikanoko.study.R
import com.shikanoko.study.data.StatisticsData
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.formatElapsed
import com.shikanoko.study.data.loadStatistics
import com.shikanoko.study.data.resetStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Persistent statistics: cumulative totals on top, per-word accuracy (weakest first) below.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<StatisticsData?>(null) }

    LaunchedEffect(Unit) {
        stats = withContext(Dispatchers.IO) { loadStatistics(context) }
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
            val data = stats

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.stats_total_time, formatElapsed((data?.totalTimeMillis ?: 0L) / 1000)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.stats_tests_taken, data?.testsTaken ?: 0),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(
                            R.string.stats_overall_accuracy,
                            accuracyPercent(data?.totalCorrect ?: 0, data?.totalSeen ?: 0)
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    stringResource(R.string.menu_stats_name),
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { resetStatistics(context) }
                        stats = withContext(Dispatchers.IO) { loadStatistics(context) }
                    }
                }) {
                    Text(stringResource(R.string.stats_reset))
                }
            }
            HorizontalDivider()

            if (data == null || data.perWord.isEmpty()) {
                Spacer(Modifier.size(16.dp))
                Text(stringResource(R.string.stats_empty))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = data.perWord) { word ->
                        ListItem(
                            headlineContent = { Text(word.prompt) },
                            supportingContent = { Text(word.answer) },
                            trailingContent = {
                                Text(
                                    stringResource(
                                        R.string.stats_word_accuracy,
                                        word.timesCorrect,
                                        word.timesSeen,
                                        accuracyPercent(word.timesCorrect, word.timesSeen)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
