package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.db.Card
import com.shikanoko.study.data.db.getDaoInstance
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import com.shikanoko.study.data.model.toStudyWord
import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.DailyCounters
import com.shikanoko.study.data.srs.ReviewCandidate
import com.shikanoko.study.data.srs.ReviewQueue
import com.shikanoko.study.data.srs.SchedulerState
import com.shikanoko.study.data.srs.WordKey

// New = never studied (no card row yet, or a row still in the NEW phase). Picks the daily counter.
val ReviewCandidate.isNew: Boolean
    get() = state == null || state.phase == CardPhase.NEW

// A loaded review session: the ordered [queue] to study now, plus the full per-direction word pools
// (whole scope, not limited) used to draw multiple-choice distractors in card mode.
data class ReviewSession(
    val queue: List<ReviewCandidate>,
    val pools: Map<Direction, List<StudyWord>>
)

// One source word expanded for one direction, with its stable key.
private data class Source(val wordKey: String, val direction: Direction, val studyWord: StudyWord)

// SQLite caps bound variables (~999); chunk key lookups well under that.
private const val KEY_LOOKUP_CHUNK = 500

// Builds the review session for [settings]. Blocking (assets / DB) — call on Dispatchers.IO.
// Expands every source word into one candidate per selected direction, attaches existing scheduler
// state, then defers ordering + daily limits to the pure ReviewQueue builder.
suspend fun buildReviewSession(context: Context, settings: TestingSettings): ReviewSession {
    val sources = loadSources(context, settings)

    // Pull existing cards for these words and index them by (wordKey, direction).
    val dao = getDaoInstance(context)
    val cards = sources.map { it.wordKey }.distinct()
        .chunked(KEY_LOOKUP_CHUNK)
        .flatMap { dao.getCardsForKeys(it) }
    val cardByKeyDir = cards.associateBy { it.wordKey to it.direction }

    val candidates = sources.map { src ->
        val card = cardByKeyDir[src.wordKey to src.direction]
        ReviewCandidate(src.wordKey, src.direction, src.studyWord, card?.toSchedulerState())
    }

    val counters = DailyCounters(context)
    val queue = ReviewQueue.build(
        candidates = candidates,
        now = System.currentTimeMillis(),
        newDoneToday = counters.newDoneToday,
        reviewsDoneToday = counters.reviewsDoneToday
    )
    val pools = sources.groupBy({ it.direction }, { it.studyWord })
    return ReviewSession(queue, pools)
}

private suspend fun loadSources(context: Context, settings: TestingSettings): List<Source> {
    val directions = settings.directions.ifEmpty { setOf(Direction.JP_TO_MEANING) }
    return when (settings.wordsSource) {
        WordsSource.MINNA -> {
            val all = MinnaCsvParser.loadWords(context, settings.language)
            val filtered = if (settings.lessons.isEmpty()) all
            else all.filter { it.lesson in settings.lessons }
            filtered.flatMap { w ->
                val key = WordKey.minna(settings.language, w.lesson, w.kanji)
                directions.map { dir -> Source(key, dir, w.toStudyWord(dir).copy(wordKey = key)) }
            }
        }
        WordsSource.LOCAL -> getDaoInstance(context).getAllWords().flatMap { w ->
            val key = WordKey.local(w.id)
            directions.map { dir -> Source(key, dir, w.toStudyWord(dir)) }
        }
    }
}

// Persists the result of grading [candidate]. Upserts on the (wordKey, direction) unique index, so
// the first review creates the row and later ones replace it. Blocking (DB) — call on Dispatchers.IO.
suspend fun persistReview(context: Context, candidate: ReviewCandidate, newState: SchedulerState) {
    getDaoInstance(context).upsertCard(
        Card(
            wordKey = candidate.wordKey,
            direction = candidate.direction,
            phase = newState.phase,
            dueAt = newState.dueAt,
            lastReviewedAt = newState.lastReviewedAt,
            lapses = newState.lapses,
            params = newState.params
        )
    )
}

private fun Card.toSchedulerState() =
    SchedulerState(phase, dueAt, lastReviewedAt, lapses, params)
