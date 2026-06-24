package com.shikanoko.study.data.srs

// The four answer grades a scheduler understands. For the current slice the UI only produces
// AGAIN (wrong) and GOOD (correct); HARD/EASY are part of the contract for a future self-grade bar.
enum class Grade { AGAIN, HARD, GOOD, EASY }
