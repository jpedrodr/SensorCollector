package com.jpdr.sensorcollector

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private var sensorEventListener: SensorEventListener? = null
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val accelerometerSensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//         Example of a call to a native method

//        stringFromJNI()
        setContent {
            SensorCollectorApp(
                onStartCollecting = ::startCollectingData,
                onStopCollecting = ::stopCollectingData
            )
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

        const val ROOT_PATH = "/SensorCollector/collects"
        const val FILE_NAME_PREFIX = "accelerometer_report_"
    }

    /**
     * Creates and returns the directory for the session if it doesn't exists
     * Just returns it otherwise
     */
    private fun createSessionDirectoryIfNeeded(sessionName: String): File {
        val rootDir = File(getExternalFilesDir(null), ROOT_PATH)

        val sessionDir = File(rootDir, sessionName.trim())

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    /**
     * Creates the new report file based on the number of report files
     */
    private fun createReportFile(sessionDir: File): File {
        val reportsDir = File(sessionDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val numberOfFiles =
            reportsDir.listFiles()?.count { it.isFile && it.name.contains(FILE_NAME_PREFIX) }
                ?: 0

        // if there are 0 files, next file will be ..._1, and so on
        val currentFileNumber = numberOfFiles + 1

        val reportFile = File(reportsDir, "$FILE_NAME_PREFIX$currentFileNumber.csv")

        return reportFile
    }

    private fun startCollectingData(sessionName: String) {
        val sessionDir = createSessionDirectoryIfNeeded(sessionName)

        val reportFile = createReportFile(sessionDir)

        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val timestamp = System.nanoTime()
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]

                        println("joaorosa | x=$x, y=$y, z=$z")
                        writeToCSV(timestamp, x, y, z)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do nothing since we don't need to handle accuracy changes
            }
        }
        sensorManager.registerListener(
            sensorEventListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun stopCollectingData() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun writeToCSV(timestamp: Long, x: Float, y: Float, z: Float) {
        // TODO joaorosa
    }
}

@Composable
fun SensorCollectorApp(
    onStartCollecting: (sessionName: String) -> Unit,
    onStopCollecting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sensor Collector App", style = MaterialTheme.typography.h5)

        val frequencies = listOf("100Hz", "200Hz", "MAX")

        val (sessionName, setSessionName) = remember { mutableStateOf("") }
        val (selectedFrequency, setSelectedFrequency) = remember { mutableStateOf(frequencies.first()) }
        val (isDropdownExpanded, setDropdownExpanded) = remember { mutableStateOf(false) }
        val (isRunning, setIsRunning) = remember { mutableStateOf(false) }

        // Session name input
        Text("Session Name:")
        TextField(
            value = sessionName,
            onValueChange = { setSessionName(it) },
            label = { Text("Enter Session Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Box {
            // Frequency selection
            Button(
                onClick = { setDropdownExpanded(!isDropdownExpanded) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = "Select Frequency")
            }

            // Show the dropdown menu only when it's expanded
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { setDropdownExpanded(false) }
            ) {
                frequencies.forEach {
                    DropdownMenuItem(onClick = {
                        setSelectedFrequency(it)
                        setDropdownExpanded(false) // Close dropdown after selection
                    }) {
                        Text(text = it)
                    }
                }
            }
        }

        Text(
            text = "Selected Frequency: $selectedFrequency",
            modifier = Modifier
                .padding(16.dp)
        )

        val collectButtonText = if (isRunning) {
            "Stop Collection"
        } else {
            "Start Collection"
        }

        val collectButtonColors = if (isRunning) {
            ButtonDefaults.buttonColors(
                backgroundColor = Color.Red,
                contentColor = Color.White
            )
        } else {
            ButtonDefaults.buttonColors(
                backgroundColor = Color.Green,
                contentColor = Color.Black
            )
        }

        // Start and Stop buttons
        Button(
            modifier = Modifier.padding(top = 16.dp),
            colors = collectButtonColors,
            onClick = {
                val newState = !isRunning
                setIsRunning(newState)

                if (newState) {
                    onStartCollecting(sessionName)
                } else {
                    onStopCollecting()
                }
            }
        ) {
            Text(collectButtonText)
        }
    }


}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SensorCollectorApp({}, {})
}