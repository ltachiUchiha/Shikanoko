package com.shikanoko.study.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shikanoko.study.R
import com.shikanoko.study.data.accuracyPercent
import com.shikanoko.study.data.db.Card as DbCard
import com.shikanoko.study.data.db.WordStat
import com.shikanoko.study.data.formatDate
import com.shikanoko.study.data.loadWordDetail
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.Sm2Params
import com.shikanoko.study.data.srs.Sm2Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val DAY_MS = 86_400_000L

// Detailed per-word statistics: accuracy breakdown, streaks, timeline, and (when the word has been
// reviewed) its SRS schedule. The accuracy fields come from the [stat] passed in; the schedule is
// loaded from the matching SRS card.
@Composable
fun WordStatDetailScreen(stat: WordStat, onBack: () -> Unit) {
    val context = LocalContext.current
    var card by remember { mutableStateOf<DbCard?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(stat.wordKey, stat.direction) {
        card = withContext(Dispatchers.IO) { loadWordDetail(context, stat.wordKey, stat.direction)?.card }
        loaded = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.stats_back)
                    )
                }
                Text(
                    stringResource(directionLabelRes(stat.direction)),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.size(8.dp))
            Text(
                text = stat.prompt,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stat.answer,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(16.dp))

            // Accuracy: prominent percentage, then the underlying counts.
            DetailCard {
                Text(
                    stringResource(R.string.stats_percent, accuracyPercent(stat.timesCorrect, stat.timesSeen)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.stats_accuracy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(8.dp))
                DetailRow(stringResource(R.string.stats_seen), stat.timesSeen.toString())
                DetailRow(stringResource(R.string.stats_correct), stat.timesCorrect.toString())
                DetailRow(stringResource(R.string.stats_wrong), (stat.timesSeen - stat.timesCorrect).toString())
            }

            Spacer(Modifier.size(8.dp))

            // Streaks + timeline.
            DetailCard {
                DetailRow(stringResource(R.string.stats_current_streak), stat.currentStreak.toString())
                DetailRow(stringResource(R.string.stats_best_streak), stat.bestStreak.toString())
                DetailRow(stringResource(R.string.stats_first_studied), dateOrDash(stat.firstTestedAt))
                DetailRow(stringResource(R.string.stats_last_studied), dateOrDash(stat.lastTestedAt))
            }

            Spacer(Modifier.size(8.dp))

            // SRS schedule (only meaningful once the word has been reviewed).
            DetailCard {
                Text(
                    stringResource(R.string.stats_schedule),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                val c = card
                when {
                    !loaded -> {}
                    c == null -> Text(
                        stringResource(R.string.stats_not_in_review),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        val p = Sm2Params.decode(c.params)
                        DetailRow(stringResource(R.string.stats_status), stringResource(phaseLabelRes(c.phase)))
                        DetailRow(stringResource(R.string.stats_next_review), dueLabel(c.dueAt))
                        DetailRow(stringResource(R.string.stats_interval), intervalLabel(c.phase, p.intervalDays))
                        DetailRow(stringResource(R.string.stats_ease), stringResource(R.string.stats_ease_value, p.easeFactor))
                        DetailRow(stringResource(R.string.stats_successful_reviews), p.repetitions.toString())
                        DetailRow(stringResource(R.string.stats_lapses), c.lapses.toString())
                        if (Sm2Scheduler.isLeech(c.lapses)) {
                            Spacer(Modifier.size(4.dp))
                            Text(
                                stringResource(R.string.stats_leech),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

// Label on the left, value on the right.
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

// Relative phrasing near-term, falling back to the concrete date further out.
@Composable
private fun dueLabel(dueAt: Long): String {
    val diff = dueAt - System.currentTimeMillis()
    val days = diff / DAY_MS
    return when {
        diff < 0 -> stringResource(R.string.stats_due_overdue)
        days == 0L -> stringResource(R.string.stats_due_today)
        days == 1L -> stringResource(R.string.stats_due_tomorrow)
        else -> formatDate(dueAt)
    }
}

// Day-scale interval once in REVIEW; otherwise the card is still on minute-scale learning steps.
@Composable
private fun intervalLabel(phase: CardPhase, intervalDays: Double): String =
    if (phase == CardPhase.REVIEW && intervalDays >= 1.0)
        stringResource(R.string.stats_interval_days, intervalDays.roundToInt())
    else
        stringResource(R.string.stats_interval_learning)

private fun dateOrDash(millis: Long): String = formatDate(millis).ifBlank { "—" }

internal fun directionLabelRes(direction: Direction): Int = when (direction) {
    Direction.JP_TO_MEANING -> R.string.settings_dir_jp_meaning
    Direction.MEANING_TO_JP -> R.string.settings_dir_meaning_jp
}

private fun phaseLabelRes(phase: CardPhase): Int = when (phase) {
    CardPhase.NEW -> R.string.stats_phase_new
    CardPhase.LEARNING -> R.string.stats_phase_learning
    CardPhase.REVIEW -> R.string.stats_phase_review
    CardPhase.RELEARNING -> R.string.stats_phase_relearning
}
