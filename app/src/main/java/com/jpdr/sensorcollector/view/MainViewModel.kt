package com.jpdr.sensorcollector.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jpdr.sensorcollector.R
import com.jpdr.sensorcollector.SensorManager
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
    private val sensorManager: SensorManager
) : AndroidViewModel(context) {

    init {
        System.loadLibrary("sensor_analyzer")
    }

//    private val sensorManager: SensorManager = SensorManager(context)

    val formatter = DecimalFormat("0.##E0", DecimalFormatSymbols(Locale.US))

    private val _state = MutableStateFlow<SensorCollectorState>(
        SensorCollectorState(
            availableFrequencies = FREQUENCIES,
            selectedFrequency = FREQUENCIES.first()
        )
    )
    val state = _state.asStateFlow()

    external fun createReport(sessionName: String)

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.UpdateSessionName -> {
                _state.update {
                    it.copy(sessionName = intent.sessionName.trim())
                }
            }

            is MainIntent.SelectFrequency -> {
                _state.update {
                    it.copy(
                        selectedFrequency = intent.frequency,
                        isDropdownMenuExpanded = false
                    )
                }
            }

            is MainIntent.ToggleDropdownMenu -> {
                _state.update {
                    it.copy(isDropdownMenuExpanded = intent.expanded)
                }
            }

            is MainIntent.DisplaySessionData -> {
                if (_state.value.sessionName.isEmpty()) {
                    _state.update {
                        it.copy(errorRes = R.string.error_empty_session_name)
                    }
                    return
                }

//                val data = sensorManager.getSessionData(_state.value.sessionName)
//
//                if (data.isEmpty()) {
//                    _state.update {
//                        it.copy(
//                            errorRes = R.string.error_empty_session_data,
//                            sessionData = null
//                        )
//                    }
//                } else {
//                   val formattedData = getFormattedSessionData(data)
//
//                    _state.update {
//                        it.copy(sessionData = formattedData)
//                    }
//                }
            }

            is MainIntent.DisplayLastReportData -> {
//                val data = sensorManager.getLastReportData(_state.value.sessionName)
//                _state.update {
//                    it.copy(reportData = data)
//                }
            }

            is MainIntent.StartCollecting -> {
//                if (_state.value.sessionName.isEmpty()) {
//                    _state.update {
//                        it.copy(errorRes = R.string.error_empty_session_name)
//                    }
//                } else {
//                    _state.update {
//                        it.copy(
//                            isCollecting = true,
//                            errorRes = null,
//                            sessionData = null
//                        )
//                    }
//                    sensorManager.startCollectingData(_state.value.sessionName)
//                    startReporting(_state.value.sessionName)
//                }
            }

            MainIntent.StopCollecting -> {
//                _state.update {
//                    it.copy(isCollecting = false)
//                }
//
//                sensorManager.stopCollectingData()
//                stopReporting(_state.value.sessionName)
            }
        }
    }

    fun getFormattedSessionData(data: List<List<String>>): List<List<String>> {
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
        println("joaorosa | startReporting -> $sessionName")
        viewModelScope.launch(Dispatchers.IO) {
            while (_state.value.isCollecting) {
                delay(INTERVAL_MILLIS)
                createReport(sessionName)
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