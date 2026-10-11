package com.example.anvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnviApp()
        }
    }
}

@Composable
fun AnviApp() {
    var response by remember {
        mutableStateOf("System ready. Tap the button to test ANVI.")
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ANVI",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Android Controller",
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = response,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Button(
                    onClick = {
                        response = "Hello! ANVI is running. Button test successful."
                    },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Test ANVI")
                }
            }
        }
    }
}