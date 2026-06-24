package com.shikanoko.study

import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.srs.WordKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WordKeyTest {

    @Test
    fun minnaKeyIncludesLanguageLessonAndWord() {
        assertEquals("minna:en:5:食べる", WordKey.minna(MinnaLanguage.EN, 5, "食べる"))
        assertEquals("minna:ru:12:本", WordKey.minna(MinnaLanguage.RU, 12, "本"))
    }

    @Test
    fun minnaKeyDiffersByLanguageSoProgressIsTrackedSeparately() {
        assertNotEquals(
            WordKey.minna(MinnaLanguage.EN, 5, "食べる"),
            WordKey.minna(MinnaLanguage.RU, 5, "食べる")
        )
    }

    @Test
    fun localKeyUsesRowId() {
        assertEquals("local:42", WordKey.local(42))
    }
}
