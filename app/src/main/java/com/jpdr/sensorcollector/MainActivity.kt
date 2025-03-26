package com.jpdr.sensorcollector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {

    //    private lateinit var sensorManager: SensorManager
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//         Example of a call to a native method
//        stringFromJNI()
//
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            SensorCollectorApp(
                state = state,
                handleIntent = {
                    viewModel.handleIntent(it)
                }
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

    }
}

@Composable
fun SensorCollectorApp(
    state: SensorCollectorState,
    handleIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sensor Collector App", style = MaterialTheme.typography.h5)

        // Session name input
        TextField(
            value = state.sessionName,
            onValueChange = { handleIntent(MainIntent.UpdateSessionName(it)) },
            label = { Text("Enter Session Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            enabled = !state.isCollecting
        )

        state.errorRes?.let {
            Text(
                stringResource(state.errorRes),
                color = Color.Red,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box {
            // Frequency selection
            Button(
                onClick = {
                    handleIntent(MainIntent.ToggleDropdownMenu(!state.isDropdownMenuExpanded))
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = "Select Frequency")
            }

            // Show the dropdown menu only when it's expanded
            DropdownMenu(
                expanded = state.isDropdownMenuExpanded,
                onDismissRequest = { handleIntent(MainIntent.ToggleDropdownMenu(false)) }
            ) {
                state.availableFrequencies.forEach {
                    DropdownMenuItem(
                        onClick = {
                            handleIntent(MainIntent.SelectFrequency(it))
                            // Close dropdown after selection
                        }
                    ) {
                        Text(text = it)
                    }
                }
            }
        }


        Text(
            text = "${stringResource(R.string.selected_frequency)} ${state.selectedFrequency}",
            modifier = Modifier
                .padding(16.dp)
        )

        val collectButtonText = if (state.isCollecting) {
            stringResource(R.string.stop_collecting)
        } else {
            stringResource(R.string.start_collecting)
        }

        val collectButtonColors = if (state.isCollecting) {
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

        // Start/ Stop button
        Button(
            modifier = Modifier.padding(top = 16.dp),
            colors = collectButtonColors,
            onClick = {
                val newState = !state.isCollecting

                if (newState) {
                    handleIntent(MainIntent.StartCollecting)
                } else {
                    handleIntent(MainIntent.StopCollecting)
                }
            }
        ) {
            Text(collectButtonText)
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                handleIntent(MainIntent.DisplaySessionData)
            }
        ) {
            Text(stringResource(R.string.view_session))
        }

        state.sessionData?.let { session ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(5), // 5 fields, 1 column per field
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color.Black)
            ) {
                items(session.flatten()) { cell ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(text = cell)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SensorCollectorAppPreview() {
    SensorCollectorApp(
        state = SensorCollectorState(),
        handleIntent = {}
    )
}