package com.jpdr.sensorcollector.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jpdr.sensorcollector.R
import com.jpdr.sensorcollector.SensorAnalyzer
import com.jpdr.sensorcollector.manager.FileManager
import com.jpdr.sensorcollector.manager.SensorManager
import com.jpdr.sensorcollector.manager.SensorManager.SensorFrequency
import com.jpdr.sensorcollector.util.FrequencyCalculator.hertzToMicroseconds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    context: Application,
    private val sensorManager: SensorManager,
    private val fileManager: FileManager,
    private val sensorAnalyzer: SensorAnalyzer
) : AndroidViewModel(context) {

    private var collectionJob: Job? = null

    private val formatter = DecimalFormat("0.##E0", DecimalFormatSymbols(Locale.US))

    private val _state = MutableStateFlow<SensorCollectorState>(
        SensorCollectorState(
            availableFrequencies = getAvailableFrequencies(),
            selectedFrequency = getAvailableFrequencies().first()
        )
    )
    val state = _state.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.UpdateSessionName -> onUpdateSessionName(intent.sessionName.trim())
            is MainIntent.SelectFrequency -> onSelectFrequency(intent.frequency)
            is MainIntent.ToggleDropdownMenu -> onToggleDropdownMenu(intent.expanded)
            is MainIntent.DisplaySessionData -> onDisplaySessionData()
            is MainIntent.DisplayLastReportData -> onDisplayLastReportData()
            is MainIntent.StartCollecting -> onStartCollecting()
            MainIntent.StopCollecting -> onStopCollecting()
        }
    }

    private fun onUpdateSessionName(sessionName: String) {
        _state.update {
            it.copy(sessionName = sessionName.trim(), errorRes = null)
        }
    }

    private fun onSelectFrequency(frequency: Frequency) {
        _state.update {
            it.copy(
                selectedFrequency = frequency,
                isDropdownMenuExpanded = false
            )
        }
    }

    private fun onToggleDropdownMenu(expanded: Boolean) {
        _state.update {
            it.copy(isDropdownMenuExpanded = expanded)
        }
    }

    private fun onDisplaySessionData() {
        if (_state.value.sessionName.isEmpty()) {
            _state.update {
                it.copy(errorRes = R.string.error_empty_session_name)
            }
            return
        }

        val data = fileManager.getSessionData(_state.value.sessionName)
        if (data.isEmpty()) {
            _state.update {
                it.copy(
                    errorRes = R.string.error_empty_session_data,
                    sessionData = null
                )
            }
        } else {
            val formattedData = getFormattedSessionData(data)
            _state.update {
                it.copy(sessionData = formattedData)
            }
        }
    }

    private fun onDisplayLastReportData() {
        if (_state.value.sessionName.isEmpty()) {
            _state.update {
                it.copy(errorRes = R.string.error_empty_session_name)
            }
            return
        }

        val reportData = fileManager.getLastReportData(_state.value.sessionName)
        if (reportData.isEmpty()) {
            _state.update {
                it.copy(
                    errorRes = R.string.error_empty_report_data,
                    reportData = null
                )
            }
        } else {
            _state.update {
                it.copy(reportData = reportData)
            }
        }
    }

    private fun onStartCollecting() {
        if (_state.value.sessionName.isEmpty()) {
            _state.update {
                it.copy(errorRes = R.string.error_empty_session_name)
            }
        } else {
            _state.update {
                it.copy(
                    isCollecting = true,
                    errorRes = null,
                    sessionData = null,
                    reportData = null
                )
            }

            val sensorFrequency =
                _state.value.selectedFrequency?.sensorFrequency ?: SensorFrequency.ONE_HUNDRED

            sensorManager.startCollectingData(
                sessionName = _state.value.sessionName,
                frequency = sensorFrequency
            )
            startReporting(
                sessionName = _state.value.sessionName,
                frequency = sensorFrequency
            )
        }
    }

    private fun onStopCollecting() {
        _state.update {
            it.copy(isCollecting = false)
        }
        sensorManager.stopCollectingData()
        stopReporting()
    }

    private fun getFormattedSessionData(data: List<List<String>>): List<List<String>> {
        return data.mapIndexed { index, row ->
            if (index == 0) row
            else {
                val timeStamp = row.firstOrNull() ?: return@mapIndexed row
                val updatedRow = listOf(
                    formatTimestamp(timeStamp.toLong())
                ) + row.drop(1)
                updatedRow
            }
        }
    }

    private fun startReporting(sessionName: String, frequency: SensorFrequency) {
        val delayInMicroseconds = when (frequency) {
            SensorFrequency.ONE_HUNDRED -> hertzToMicroseconds(100) // 100Hz
            SensorFrequency.TWO_HUNDRED -> hertzToMicroseconds(200) // 200Hz
            SensorFrequency.MAX -> sensorManager.getMaxSensorDelayInMicroseconds()
        }

        // once we stop collecting, the
        collectionJob = viewModelScope.launch(Dispatchers.IO) {
            // job was cancelled so this could be while(true) but this was is safer
            while (_state.value.isCollecting) {
                // start reporting after the report delay, no point reporting right after starting
                delay(REPORT_INTERVAL_MILLIS)
                sensorAnalyzer.createReport(
                    sessionName = sessionName,
                    delayInMicroseconds = delayInMicroseconds
                )
            }
        }
    }

    private fun stopReporting() {
        collectionJob?.cancel()
        collectionJob = null
    }

    private fun getAvailableFrequencies() = listOf(
        Frequency(SensorFrequency.ONE_HUNDRED, R.string.frequency_100),
        Frequency(SensorFrequency.TWO_HUNDRED, R.string.frequency_200),
        Frequency(SensorFrequency.MAX, R.string.frequency_max)
    )

    companion object {
        private const val REPORT_INTERVAL_MILLIS = 15 * 60 * 1000L // 15 minutes in milliseconds
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            formatter.format(timestamp)
        } catch (_: NumberFormatException) {
            timestamp.toString() // Return original if not a valid number
        }
    }
}

data class Frequency(
    val sensorFrequency: SensorFrequency,
    @StringRes val displayValue: Int
)

data class SensorCollectorState(
    val sessionName: String = "",
    val availableFrequencies: List<Frequency> = listOf(),
    val selectedFrequency: Frequency? = null,
    val isDropdownMenuExpanded: Boolean = false,
    val isCollecting: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val sessionData: List<List<String>>? = null,
    val reportData: List<List<String>>? = null,
)

sealed class MainIntent() {
    data class UpdateSessionName(val sessionName: String) : MainIntent()
    data class SelectFrequency(val frequency: Frequency) : MainIntent()
    data class ToggleDropdownMenu(val expanded: Boolean) : MainIntent()
    data object StartCollecting : MainIntent()
    data object StopCollecting : MainIntent()
    data object DisplaySessionData : MainIntent()
    data object DisplayLastReportData : MainIntent()
}