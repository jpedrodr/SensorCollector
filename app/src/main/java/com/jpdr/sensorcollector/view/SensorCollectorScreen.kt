package com.jpdr.sensorcollector.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jpdr.sensorcollector.R
import com.jpdr.sensorcollector.manager.SensorManager.SensorFrequency

@Composable
fun SensorCollectorScreen(
    state: SensorCollectorState,
    handleIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.h5)

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
                enabled = !state.isCollecting,
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
                        }
                    ) {
                        Text(text = stringResource(it.displayValue))
                    }
                }
            }
        }

        state.selectedFrequency?.let { frequency ->
            Text(
                text = "${stringResource(R.string.selected_frequency)} ${stringResource(frequency.displayValue)}",
                modifier = Modifier
                    .padding(16.dp)
            )
        }

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

        // Start/Stop button
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
            SensorCollectorGrid(data = session)
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                handleIntent(MainIntent.DisplayLastReportData)
            }
        ) {
            Text(stringResource(R.string.view_last_report))
        }

        state.reportData?.let { report ->
            SensorCollectorGrid(data = report)
        }
    }
}

@Composable
private fun SensorCollectorGrid(data: List<List<String>>, modifier: Modifier = Modifier) {
    val header = data.firstOrNull() ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        header.forEach { headerItem ->
            Text(text = headerItem, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
    }

    val dataRows = data.drop(1)

    LazyVerticalGrid(
        columns = GridCells.Fixed(header.size),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .border(width = 1.dp, color = Color.Black)
    ) {
        items(dataRows.flatten()) { dataItem ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(text = dataItem)
            }
        }
    }
}

private val STATE_WITH_DATA = SensorCollectorState(
    sessionName = "session1",
    availableFrequencies = listOf(
        Frequency(SensorFrequency.ONE_HUNDRED, R.string.frequency_100),
        Frequency(SensorFrequency.TWO_HUNDRED, R.string.frequency_200),
        Frequency(SensorFrequency.MAX, R.string.frequency_max)
    ),
    selectedFrequency = Frequency(SensorFrequency.ONE_HUNDRED, R.string.frequency_100),
    isCollecting = true,
    sessionData = listOf(
        listOf(
            "timestamp (ns)",
            "sensor_id",
            "sensor_value_x (m/s2)",
            "sensor_value_y (m/s2)",
            "sensor_value_z (m/s2)"
        ),
        listOf("4.77E14", "1", "0.43412341", "1.43234", "1.12323"),
        listOf("4.77E14", "1", "0.43412341", "1.43234", "1.12323"),
        listOf("4.77E14", "1", "0.43412341", "1.43234", "1.12323")
    ),
    reportData = listOf(
        listOf(
            "report_timestamp",
            "collection_start_time",
            "total_samples",
            "total_gaps_duration"
        ),
        listOf("2024-12-13T00:00:00", "2024-12-13T00:00:00", "850", "10.5")
    )
)

@Preview(showBackground = true)
@Composable
fun SensorCollectorScreenDefaultPreview() {
    SensorCollectorScreen(
        state = SensorCollectorState(),
        handleIntent = {}
    )
}

@Preview(showBackground = true)
@Composable
fun SensorCollectorScreenPreview() {
    SensorCollectorScreen(
        state = STATE_WITH_DATA,
        handleIntent = {}
    )
}


@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p)
@Composable
fun SensorCollectorScreenLandscapePreview() {
    SensorCollectorScreen(
        state = STATE_WITH_DATA,
        handleIntent = {}
    )
}