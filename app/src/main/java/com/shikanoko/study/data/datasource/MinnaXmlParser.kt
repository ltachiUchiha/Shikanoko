package com.shikanoko.study.data.datasource

import android.content.Context
import android.content.res.XmlResourceParser
import com.shikanoko.study.data.model.MinnaWord
import org.xmlpull.v1.XmlPullParser

// Xml parser for working with words from "Minna no Nihongo"
class MinnaXmlParser(private val parser: XmlResourceParser) {
    fun loadData(context: Context): MutableList<MinnaWord> {

        val wordsList = mutableListOf<MinnaWord>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if(parser.name == "row"){
                parser.next()
                wordsList.add(readWord(parser))
            }

        }
        return wordsList
    }

    private fun readWord(parser: XmlPullParser): MinnaWord {
        val word = MinnaWord()
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