package com.shikanoko.study.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shikanoko.study.R
import com.shikanoko.study.data.TestType
import com.shikanoko.study.data.TestingSettings
import com.shikanoko.study.data.WordsSource

@Composable
fun TestingSettingsDialog(onDismiss: () -> Unit, onConfirm: (TestingSettings) -> Unit){
    val testingSettings = remember { mutableStateOf(TestingSettings()) }
    Dialog(
        onDismissRequest = { onDismiss }) {
        Card (modifier = Modifier.fillMaxWidth()
            .fillMaxHeight(0.5f)
            .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(text = stringResource(R.string.settings_name_popup),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .padding(8.dp)
                    .fillMaxHeight()){

                Text("Words source")
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    val buttonTextList = listOf("Local database", "Minna no Nihongo")
                    val buttonText = remember{mutableStateOf(buttonTextList[0])}
                    Button(onClick = { expanded = !expanded }) {
                        Text(buttonText.value)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Local database") },
                            onClick = {
                                testingSettings.value.wordsSource = WordsSource.LOCAL
                                buttonText.value = buttonTextList[0]
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Minna no Nihongo") },
                            onClick = {
                                testingSettings.value.wordsSource = WordsSource.MINNA
                                buttonText.value = buttonTextList[1]
                                expanded = false
                            }
                        )
                    }
                }

                Text("Test by enter")
                var checkTypeOfTest by remember { mutableStateOf(false) }

                Checkbox(checkTypeOfTest, onCheckedChange = {checkTypeOfTest = it}, enabled = true)

                Row(verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()){
                    TextButton(onClick = { onDismiss }) {
                        Text("Close")
                    }

                    TextButton(onClick = {
                        when(checkTypeOfTest){
                            true -> { testingSettings.value.testType = TestType.CARD }
                            false -> { testingSettings.value.testType = TestType.TEXT }
                        }
                        onConfirm(testingSettings.value)
                    }) {

                        Text("Confirm")
                    }
                }
            }
        }
    }
}