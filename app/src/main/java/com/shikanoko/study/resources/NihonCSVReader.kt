package com.shikanoko.study.resources

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.shikanoko.study.R
import org.xmlpull.v1.XmlPullParser

// Data class for words from "Minna no Nihongo"
data class NihonCSVWord(
    val kanji: String,
    val kana: String,
    val translation: String,
    val lesson: Int,
    val add: Boolean
)

// Repository for working with words from "Minna no Nihongo"
class NihonCSVRepository {
    fun getAllWords(context: Context){
        val parser = context.resources.getXml(R.xml.nihon)
        //parser.require(XmlPullParser.START_TAG, null, "resources")

        var kanji: String = ""
        var kana: String = ""
        var translation: String = ""
        var lesson: Int = 0
        var add: Boolean = false
        var text = ""
        val wordsList = mutableListOf<NihonCSVWord>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            if (parser.eventType == XmlPullParser.TEXT) {
                text = parser.text
            }

            else if (parser.eventType == XmlPullParser.END_TAG){
                when (parser.name){
                    "Kanji" -> kanji = text
                    "Kana" -> kana = text
                    "Translation" -> translation = text
                    "Lesson" -> lesson = text.toInt()
                    "Add" -> add = text.lowercase().toBoolean()
                    "row" ->  wordsList.add(NihonCSVWord(kanji, kana, translation, lesson, add))
                }
            }
        }

        Toast.makeText(context, "End", Toast.LENGTH_SHORT).show()
    }

}