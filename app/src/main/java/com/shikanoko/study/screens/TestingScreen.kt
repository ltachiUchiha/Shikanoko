package com.shikanoko.study.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shikanoko.study.R

@Composable
fun TestingScreen(){
    Surface (modifier = Modifier
        .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            KanjiCard()
            TestCard()
        }
    }
}
@Composable
fun KanjiCard(){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .padding(top = 34.dp)
            .fillMaxWidth()
    ){
        Image(painter = painterResource(id = R.drawable.shika), contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(start = 12.dp)
        )
        Column (
            modifier = Modifier
                .padding(start = 12.dp)
        ) {
            Text("Noko Shikanoko", fontWeight = FontWeight.Bold)
        }

    }
}


@Composable
fun TestCard(){
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(1f)
    ){
        KanaColumn(0.5f)
        KanaColumn(1f)
    }
}

@Composable
fun KanaColumn(fraction: Float){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(0.5f)
    ){
        KanaElement(text = "Тест1", 1)
        KanaElement(text = "Тест2", 2)
        KanaElement(text = "Тест3", 3)
    }
}

@Composable
fun KanaElement(text: String, number: Int){
    Button(
        onClick = { /*TODO*/ },
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .size(100.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(30)
    ){
        Text(text)
    }
}