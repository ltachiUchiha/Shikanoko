package com.shikanoko.study

import com.shikanoko.study.data.srs.Sm2Params
import org.junit.Assert.assertEquals
import org.junit.Test

class Sm2ParamsTest {

    @Test
    fun encodeDecodeRoundTrips() {
        val original = Sm2Params(easeFactor = 2.35, intervalDays = 6.0, repetitions = 3, stepIndex = 1)
        assertEquals(original, Sm2Params.decode(original.encode()))
    }

    @Test
    fun defaultsRoundTrip() {
        val original = Sm2Params()
        assertEquals(original, Sm2Params.decode(original.encode()))
    }

    @Test
    fun decodeFallsBackToDefaultsForGarbage() {
        val defaults = Sm2Params()
        assertEquals(defaults, Sm2Params.decode(""))
        assertEquals(defaults, Sm2Params.decode("not|valid|data|here"))
    }

    @Test
    fun decodeFillsMissingTrailingFieldsWithDefaults() {
        val p = Sm2Params.decode("1.9|6.0")
        assertEquals(1.9, p.easeFactor, 0.0001)
        assertEquals(6.0, p.intervalDays, 0.0001)
        assertEquals(0, p.repetitions)
        assertEquals(0, p.stepIndex)
    }
}
