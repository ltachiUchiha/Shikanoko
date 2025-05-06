package com.shikanoko.study.resources

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.shikanoko.study.R
import org.xmlpull.v1.XmlPullParser

// Data class for words from "Minna no Nihongo"
data class NihonCSVWord(
    var kanji: String = "",
    var kana: String = "",
    var translation: String = "",
    var lesson: Int = 0,
    var add: Boolean = false
)

// Repository for working with words from "Minna no Nihongo"
class NihonCSVRepository {
    fun getAllWords(context: Context){
        val parser = context.resources.getXml(R.xml.nihon)
        //parser.require(XmlPullParser.START_TAG, null, "resources")

        var word = NihonCSVWord()

        val wordsList = mutableListOf<NihonCSVWord>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if(parser.name == "row"){
                parser.next()
                wordsList.add(readWord(parser))
            }

        }
        Toast.makeText(context, "end", Toast.LENGTH_SHORT).show()
    }

    private fun readWord(parser: XmlPullParser): NihonCSVWord {
        val word = NihonCSVWord()
        while (parser.name != "row"){
            when (parser.name){
                "Kanji" -> word.kanji = readText(parser)
                "Kana" -> word.kana = readText(parser)
                "Translation" -> word.translation = readText(parser)
                "Lesson" -> word.lesson = readText(parser).toInt()
                "Add" -> word.add = readText(parser).lowercase().toBoolean()
            }
            parser.next()
        }
        return word
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