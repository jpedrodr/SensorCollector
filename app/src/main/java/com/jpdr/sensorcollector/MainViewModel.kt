package com.jpdr.sensorcollector

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    context: Application
) : AndroidViewModel(context) {

    init {
        System.loadLibrary("sensor_analyzer")
    }

    private val sensorManager: SensorManager = SensorManager(context)

    private val _state = MutableStateFlow<SensorCollectorState>(
        SensorCollectorState(
            availableFrequencies = FREQUENCIES,
            selectedFrequency = FREQUENCIES.first()
        )
    )
    val state = _state.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.UpdateSessionName -> {
                _state.update {
                    it.copy(sessionName = intent.sessionName)
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

                val data = sensorManager.getSessionData(_state.value.sessionName)

                if (data.isEmpty()) {
                    _state.update {
                        it.copy(
                            errorRes = R.string.error_empty_session_data,
                            sessionData = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(sessionData = data)
                    }
                }
            }

            is MainIntent.StartCollecting -> {
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

            MainIntent.StopCollecting -> {
                _state.update {
                    it.copy(isCollecting = false)
                }

                sensorManager.stopCollectingData()
                stopReporting(_state.value.sessionName)
            }
        }
    }

    external fun createReport(sessionName: String)

    private fun startReporting(sessionName: String) {
        println("joaorosa | startReporting -> $sessionName")
        viewModelScope.launch(Dispatchers.IO) {
            while (_state.value.isCollecting) {
                createReport(sessionName)
                delay(INTERVAL_MILLIS)
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
}

data class SensorCollectorState(
    val sessionName: String = "",
    val availableFrequencies: List<String> = listOf(),
    val selectedFrequency: String = "",
    val isDropdownMenuExpanded: Boolean = false,
    val isCollecting: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val sessionData: List<List<String>>? = null
)

sealed class MainIntent() {
    data class UpdateSessionName(val sessionName: String) : MainIntent()
    data class SelectFrequency(val frequency: String) : MainIntent()
    data class ToggleDropdownMenu(val expanded: Boolean) : MainIntent()
    data object StartCollecting : MainIntent()
    data object StopCollecting : MainIntent()
    data object DisplaySessionData : MainIntent()
}