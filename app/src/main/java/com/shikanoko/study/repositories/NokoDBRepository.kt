package com.shikanoko.study.repositories

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
import kotlinx.coroutines.runBlocking

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

    @Query("DELETE FROM word")
    suspend fun deleteAllWords()
}

@Database(entities = [Word::class], version = 1)
abstract class NokoDatabase : RoomDatabase() {
    abstract fun nokoDao(): NokoDao
}

private var wordDao: NokoDao? = null
fun getDaoInstance(context: Context): NokoDao {
    if (wordDao == null){
        val db = Room.databaseBuilder(context, NokoDatabase::class.java, "noko-db")
            .build()
        wordDao = db.nokoDao()
        runBlocking {
            /*
            wordDao!!.insertWord(Word(word = "すき", meaning = "любимый"))
            wordDao!!.insertWord(Word(word = "りょうり", meaning = "блюдо"))
            wordDao!!.insertWord(Word(word = "ものもの", meaning = "напиток"))
            wordDao!!.insertWord(Word(word = "かたかな", meaning = "катакана"))
            wordDao!!.insertWord(Word(word = "ひらがな", meaning = "хирагана"))
            wordDao!!.insertWord(Word(word = "かんじ", meaning = "иероглиф"))
             */
        }

        return wordDao as NokoDao
    }
    return wordDao as NokoDao
}