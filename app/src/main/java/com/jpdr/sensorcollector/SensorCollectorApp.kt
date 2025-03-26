package com.jpdr.sensorcollector

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun SensorCollectorApp(
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
            .heightIn(max = 400.dp)
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

@Preview(showBackground = true)
@Composable
fun SensorCollectorAppPreview() {
    SensorCollectorApp(
        state = SensorCollectorState(),
        handleIntent = {}
    )
}