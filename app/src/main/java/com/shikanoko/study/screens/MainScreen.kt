package com.shikanoko.study.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.shikanoko.study.resources.NihonCSVRepository

@Composable
fun MainScreen () {
    Surface (color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        val nihonCSVRepository = NihonCSVRepository()
        nihonCSVRepository.getAllWords(LocalContext.current)
    }
}