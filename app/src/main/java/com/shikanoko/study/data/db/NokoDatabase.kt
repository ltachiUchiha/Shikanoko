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
}

@Database(entities = [Word::class], version = 1)
abstract class NokoDatabase : RoomDatabase() {
    abstract fun nokoDao(): NokoDao
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
        ).build().nokoDao().also { dao ->
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