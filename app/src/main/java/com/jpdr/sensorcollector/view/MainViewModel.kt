package com.jpdr.sensorcollector.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jpdr.sensorcollector.R
import com.jpdr.sensorcollector.SensorAnalyzer
import com.jpdr.sensorcollector.manager.FileManager
import com.jpdr.sensorcollector.manager.SensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val formatter = DecimalFormat("0.##E0", DecimalFormatSymbols(Locale.US))

    private val _state = MutableStateFlow<SensorCollectorState>(
        SensorCollectorState(
            availableFrequencies = FREQUENCIES,
            selectedFrequency = FREQUENCIES.first()
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
            it.copy(sessionName = sessionName.trim())
        }
    }

    private fun onSelectFrequency(frequency: String) {
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
                    sessionData = null
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
                    sessionData = null
                )
            }
            sensorManager.startCollectingData(_state.value.sessionName)
            startReporting(_state.value.sessionName)
        }
    }

    private fun onStopCollecting() {
        _state.update {
            it.copy(isCollecting = false)
        }
        sensorManager.stopCollectingData()
        stopReporting(_state.value.sessionName)
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

    private fun startReporting(sessionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            while (_state.value.isCollecting) {
                delay(INTERVAL_MILLIS)
                sensorAnalyzer.createReport(sessionName)
            }
        }
    }

    private fun stopReporting(sessionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            stopReporting(sessionName)
        }
    }

    companion object {
        private val FREQUENCIES = listOf("100Hz", "200Hz", "MAX")

        //    private const val INTERVAL_MILLIS = 15 * 60 * 1000L // 15 minutes in milliseconds
        private const val INTERVAL_MILLIS = 1 * 6 * 1000L // 15 minutes in milliseconds // joaorosa
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            formatter.format(timestamp)
        } catch (_: NumberFormatException) {
            timestamp.toString() // Return original if not a valid number
        }
    }
}

data class SensorCollectorState(
    val sessionName: String = "",
    val availableFrequencies: List<String> = listOf(),
    val selectedFrequency: String = "",
    val isDropdownMenuExpanded: Boolean = false,
    val isCollecting: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val sessionData: List<List<String>>? = null,
    val reportData: List<List<String>>? = null,
)

sealed class MainIntent() {
    data class UpdateSessionName(val sessionName: String) : MainIntent()
    data class SelectFrequency(val frequency: String) : MainIntent()
    data class ToggleDropdownMenu(val expanded: Boolean) : MainIntent()
    data object StartCollecting : MainIntent()
    data object StopCollecting : MainIntent()
    data object DisplaySessionData : MainIntent()
    data object DisplayLastReportData : MainIntent()
}