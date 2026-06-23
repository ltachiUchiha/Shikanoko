package com.shikanoko.study.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "word") val word:String,
    @ColumnInfo(name = "meaning") val meaning:String,
    //@ColumnInfo(name = "partOfSpeech") val partOfSpeech:String
)

// Per-word testing statistics. Keyed on `prompt` (the Japanese word shown), so a
// word's accuracy aggregates across translation languages — the simplest stable
// identity that answers "how well do I know this word".
@Entity(tableName = "word_stat")
data class WordStat(
    @PrimaryKey val prompt: String,
    val answer: String,        // latest answer text, kept for display
    val timesSeen: Int,
    val timesCorrect: Int,
    val lastTestedAt: Long
)

// One completed test run — drives the cumulative "time spent" / tests-taken totals.
@Entity(tableName = "test_session")
data class TestSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationMillis: Long,
    val totalWords: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val timestamp: Long
)

@Dao
interface NokoDao {
    @Insert
    suspend fun insertWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Update
    suspend fun updateWord(word: Word)

    @Query("SELECT * FROM word")
    suspend fun getAllWords(): List<Word>

    @Query("SELECT * FROM word")
    fun observeAll(): Flow<List<Word>>

    @Query("DELETE FROM word")
    suspend fun deleteAllWords()

    // Atomic upsert: one row per word, incrementing seen/correct counts in place.
    @Query("""
        INSERT INTO word_stat (prompt, answer, timesSeen, timesCorrect, lastTestedAt)
        VALUES (:prompt, :answer, 1, :correctInc, :now)
        ON CONFLICT(prompt) DO UPDATE SET
            timesSeen = timesSeen + 1,
            timesCorrect = timesCorrect + :correctInc,
            answer = :answer,
            lastTestedAt = :now
    """)
    suspend fun recordAnswer(prompt: String, answer: String, correctInc: Int, now: Long)

    // Weakest words first (lowest accuracy, then most-seen) to surface what needs review.
    @Query("SELECT * FROM word_stat ORDER BY (timesCorrect * 1.0 / timesSeen) ASC, timesSeen DESC")
    suspend fun getAllStats(): List<WordStat>

    @Query("DELETE FROM word_stat")
    suspend fun deleteAllStats()

    @Insert
    suspend fun insertSession(session: TestSession)

    @Query("SELECT * FROM test_session")
    suspend fun getAllSessions(): List<TestSession>

    @Query("DELETE FROM test_session")
    suspend fun deleteAllSessions()
}

@Database(entities = [Word::class, WordStat::class, TestSession::class], version = 2)
abstract class NokoDatabase : RoomDatabase() {
    abstract fun nokoDao(): NokoDao
}

// Additive migration: only creates the two new stats tables, so existing local
// words are preserved. The CREATE TABLE statements must match Room's generated
// schema exactly (NOT NULL on non-null Kotlin fields, backtick-quoted names).
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `word_stat` (
                `prompt` TEXT NOT NULL,
                `answer` TEXT NOT NULL,
                `timesSeen` INTEGER NOT NULL,
                `timesCorrect` INTEGER NOT NULL,
                `lastTestedAt` INTEGER NOT NULL,
                PRIMARY KEY(`prompt`)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `test_session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `durationMillis` INTEGER NOT NULL,
                `totalWords` INTEGER NOT NULL,
                `totalAttempts` INTEGER NOT NULL,
                `correctAttempts` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Volatile
private var wordDao: NokoDao? = null
private val daoLock = Any()

fun getDaoInstance(context: Context): NokoDao {
    // Double-checked locking: safe to call from multiple coroutines/threads.
    return wordDao ?: synchronized(daoLock) {
        wordDao ?: Room.databaseBuilder(
            context.applicationContext,
            NokoDatabase::class.java,
            "noko-db"
        ).addMigrations(MIGRATION_1_2).build().nokoDao().also { dao ->
            wordDao = dao
            // To seed development data, do it off the main thread, e.g.:
             CoroutineScope(Dispatchers.IO).launch {
                 dao.insertWord(Word(word = "すき", meaning = "любимый"))
                 dao.insertWord(Word(word = "りょうり", meaning = "блюдо"))
                 dao.insertWord(Word(word = "ものもの", meaning = "напиток"))
                 dao.insertWord(Word(word = "かたかな", meaning = "катакана"))
                 dao.insertWord(Word(word = "ひらがな", meaning = "хирагана"))
                 dao.insertWord(Word(word = "かんじ", meaning = "иероглиф"))
             }
        }
    }
}