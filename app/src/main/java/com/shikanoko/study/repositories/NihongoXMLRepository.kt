package com.shikanoko.study.repositories

import android.content.Context
import com.shikanoko.study.R
import com.shikanoko.study.data.NihongoWord
import org.xmlpull.v1.XmlPullParser

// Repository for working with words from "Minna no Nihongo"
class NihongoXMLRepository {
    fun loadData(context: Context): MutableList<NihongoWord> {
        val parser = context.resources.getXml(R.xml.nihon)

        val wordsList = mutableListOf<NihongoWord>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if(parser.name == "row"){
                parser.next()
                wordsList.add(readWord(parser))
            }

        }
        return wordsList
    }

    private fun readWord(parser: XmlPullParser): NihongoWord {
        val word = NihongoWord()
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