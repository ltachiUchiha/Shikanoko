package com.shikanoko.study.resources

import android.content.Context
import android.util.Log
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

        val wordsList = mutableListOf<NihonCSVWord>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.name){
                "kanji" -> kanji = readText(parser)
                "kana" -> kana = readText(parser)
                "translation" -> translation = readText(parser)
                "lesson" -> lesson = readText(parser).toInt()
                "add" -> add = readText(parser).lowercase().toBoolean()

            }

            wordsList.add(NihonCSVWord(kanji, kana, translation, lesson, add))
        }

    }
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

}