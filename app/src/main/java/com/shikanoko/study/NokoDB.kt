package com.shikanoko.study

import android.content.Context
import androidx.compose.ui.platform.LocalContext
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

@Entity
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "word") val word:String,
    @ColumnInfo(name = "meaning") val meaning:String
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
        return wordDao as NokoDao
    }
    return wordDao as NokoDao
}