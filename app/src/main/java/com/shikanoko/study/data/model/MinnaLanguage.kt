package com.shikanoko.study.data.model

import android.content.Context

// Translation language for the "Minna no Nihongo" vocabulary (one CSV per language).
enum class MinnaLanguage {
    EN,
    RU;

    companion object {
        // Default language derived from the current app/device locale: Russian -> RU, otherwise EN.
        fun fromLocale(context: Context): MinnaLanguage {
            val lang = context.resources.configuration.locales[0].language
            return if (lang == "ru") RU else EN
        }
    }
}
