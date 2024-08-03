package com.example.simoncitypicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.citypicker.SimonCityPicker
import com.example.simoncitypicker.ui.theme.SimonCityPickerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonCityPickerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val context = LocalContext.current
                    var selectedCity by remember { mutableStateOf("请选择城市") }
                    var isCity by remember { mutableStateOf(false) }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(100.dp)
                    ) {
                        androidx.compose.material3.TextButton(modifier = Modifier.background(Color.Gray),onClick = {
                            isCity = true
                        }){
                            Text(selectedCity)
                        }
                    }
                    SimonCityPicker(
                        context = context,
                        isCity = isCity,
                        onCitySelected = { city ->
                            selectedCity=city
                            isCity = false
                        }
                    )
                }
            }
        }
    }
}