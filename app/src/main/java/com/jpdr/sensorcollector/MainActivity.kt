package com.jpdr.sensorcollector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//         Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()
        setContent {
            SensorCollectorApp(text = stringFromJNI())
        }
    }

    /**
     * A native method that is implemented by the 'sensorcollector' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'sensor_analyzer' library on application startup.
        init {
            System.loadLibrary("sensor_analyzer")
        }
    }
}

@Composable
fun SensorCollectorApp(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sensor Collector App")

        // Session name input
        Text("Session Name:")
        TextField(
            value = sessionName,
            onValueChange = { setSessionName(it) },
            label = { Text("Enter Session Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Frequency selection
        Text("Select Frequency:")
        DropdownMenu(
            expanded = true,
            onDismissRequest = { /* Dismiss menu */ }
        ) {
            listOf("100Hz", "200Hz", "MAX").forEach {
                DropdownMenuItem(onClick = { setSelectedFrequency(it) }) {
                    Text(text = it)
                }
            }
        }

        Text("Selected Frequency: $selectedFrequency")

        // Start and Stop buttons
        Button(onClick = { /* Start data collection */ }) {
            Text("Start Collection")
        }

        Button(onClick = { /* Stop data collection */ }) {
            Text("Stop Collection")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SensorCollectorApp("Some text")
}