package com.shikanoko.study.data.model

// A recall direction for a word. One word can be drilled in several directions, each with its
// own independent SRS state (its own Card). Only the two directions needed today are defined;
// KANJI_TO_READING / AUDIO_TO_MEANING are deferred to a later pass.
enum class Direction {
    JP_TO_MEANING,   // Japanese word shown -> recall the translation (recognition)
    MEANING_TO_JP    // translation shown -> recall the Japanese word (production)
}
