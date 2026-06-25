package com.shikanoko.study.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.srs.CardPhase
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

// Per-word testing statistics. Keyed on (wordKey, direction) — the same stable identity the SRS
// cards use — so accuracy stats line up with the review schedule and each recall direction is
// tracked independently. prompt/answer are kept only for display.
@Entity(tableName = "word_stat", primaryKeys = ["wordKey", "direction"])
data class WordStat(
    val wordKey: String,
    val direction: Direction,
    val prompt: String,        // latest prompt text, kept for display
    val answer: String,        // latest answer text, kept for display
    val timesSeen: Int,
    val timesCorrect: Int,
    val currentStreak: Int,    // consecutive correct answers, reset to 0 on a miss
    val bestStreak: Int,       // longest run of consecutive correct answers
    val firstTestedAt: Long,
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

// One recall direction of one word = one independently-scheduled SRS card. Keyed by a stable string
// (see WordKey) + direction instead of a Word foreign key, because Minna words live only in the CSV.
// Holds only scheduler state; the display text (prompt/answer) is rebuilt from the source at queue time.
@Entity(
    tableName = "card",
    indices = [Index(value = ["wordKey", "direction"], unique = true)]
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordKey: String,
    val direction: Direction,
    val phase: CardPhase,
    val dueAt: Long,
    val lastReviewedAt: Long?,
    val lapses: Int,
    val params: String
)

// Stores the two SRS enums as their names. Registered on the database via @TypeConverters.
class Converters {
    @TypeConverter fun directionToString(value: Direction): String = value.name
    @TypeConverter fun stringToDirection(value: String): Direction = Direction.valueOf(value)
    @TypeConverter fun phaseToString(value: CardPhase): String = value.name
    @TypeConverter fun stringToPhase(value: String): CardPhase = CardPhase.valueOf(value)
}

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

    // Atomic upsert: one row per (word, direction), incrementing seen/correct counts in place.
    // Streaks are maintained in SQL: a correct answer extends currentStreak (and possibly bestStreak),
    // a miss resets currentStreak to 0. The SET expressions read the pre-update row values.
    @Query("""
        INSERT INTO word_stat
            (wordKey, direction, prompt, answer, timesSeen, timesCorrect, currentStreak, bestStreak, firstTestedAt, lastTestedAt)
        VALUES (:wordKey, :direction, :prompt, :answer, 1, :correctInc, :correctInc, :correctInc, :now, :now)
        ON CONFLICT(wordKey, direction) DO UPDATE SET
            timesSeen = timesSeen + 1,
            timesCorrect = timesCorrect + :correctInc,
            currentStreak = CASE WHEN :correctInc = 1 THEN currentStreak + 1 ELSE 0 END,
            bestStreak = MAX(bestStreak, CASE WHEN :correctInc = 1 THEN currentStreak + 1 ELSE 0 END),
            prompt = :prompt,
            answer = :answer,
            lastTestedAt = :now
    """)
    suspend fun recordAnswer(
        wordKey: String,
        direction: Direction,
        prompt: String,
        answer: String,
        correctInc: Int,
        now: Long
    )

    // Weakest words first (lowest accuracy, then most-seen) to surface what needs review.
    @Query("SELECT * FROM word_stat ORDER BY (timesCorrect * 1.0 / timesSeen) ASC, timesSeen DESC")
    suspend fun getAllStats(): List<WordStat>

    // Single word's stat for the detail screen.
    @Query("SELECT * FROM word_stat WHERE wordKey = :wordKey AND direction = :direction")
    suspend fun getStat(wordKey: String, direction: Direction): WordStat?

    @Query("DELETE FROM word_stat")
    suspend fun deleteAllStats()

    @Insert
    suspend fun insertSession(session: TestSession)

    @Query("SELECT * FROM test_session")
    suspend fun getAllSessions(): List<TestSession>

    @Query("DELETE FROM test_session")
    suspend fun deleteAllSessions()

    // --- SRS cards ---

    // All cards for the given word keys (across directions); the loader matches by (key, direction).
    @Query("SELECT * FROM card WHERE wordKey IN (:wordKeys)")
    suspend fun getCardsForKeys(wordKeys: List<String>): List<Card>

    // The SRS card for one word + direction, used to show the review schedule on the detail screen.
    @Query("SELECT * FROM card WHERE wordKey = :wordKey AND direction = :direction LIMIT 1")
    suspend fun getCard(wordKey: String, direction: Direction): Card?

    // Insert-or-replace on the unique (wordKey, direction) index, so the caller never needs to track
    // the row id between a card's creation and its later updates.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: Card)

    @Query("DELETE FROM card")
    suspend fun deleteAllCards()
}

@Database(entities = [Word::class, WordStat::class, TestSession::class, Card::class], version = 4)
@TypeConverters(Converters::class)
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

// Additive migration: adds the SRS `card` table. The CREATE TABLE and the unique-index name must
// match Room's generated schema exactly (index name is `index_<table>_<col1>_<col2>`).
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `card` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `wordKey` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `phase` TEXT NOT NULL,
                `dueAt` INTEGER NOT NULL,
                `lastReviewedAt` INTEGER,
                `lapses` INTEGER NOT NULL,
                `params` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_card_wordKey_direction` " +
                "ON `card` (`wordKey`, `direction`)"
        )
    }
}

// Re-keys word_stat from `prompt` to the stable (wordKey, direction) identity and adds the streak /
// first-tested columns. The key shape changes, so the old rows can't be carried over — the table is
// dropped and recreated, resetting accumulated accuracy once. The CREATE TABLE must match Room's
// generated schema exactly (column order, NOT NULL, composite PRIMARY KEY).
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `word_stat`")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `word_stat` (
                `wordKey` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `prompt` TEXT NOT NULL,
                `answer` TEXT NOT NULL,
                `timesSeen` INTEGER NOT NULL,
                `timesCorrect` INTEGER NOT NULL,
                `currentStreak` INTEGER NOT NULL,
                `bestStreak` INTEGER NOT NULL,
                `firstTestedAt` INTEGER NOT NULL,
                `lastTestedAt` INTEGER NOT NULL,
                PRIMARY KEY(`wordKey`, `direction`)
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().nokoDao().also { dao ->
            wordDao = dao
            // To seed development data, do it off the main thread, e.g.:
            // CoroutineScope(Dispatchers.IO).launch {
            //     dao.insertWord(Word(word = "すき", meaning = "любимый"))
            //     dao.insertWord(Word(word = "りょうり", meaning = "блюдо"))
            //     dao.insertWord(Word(word = "ものもの", meaning = "напиток"))
            //     dao.insertWord(Word(word = "かたかな", meaning = "катакана"))
            //     dao.insertWord(Word(word = "ひらがな", meaning = "хирагана"))
            //     dao.insertWord(Word(word = "かんじ", meaning = "иероглиф"))
            // }
        }
    }
}