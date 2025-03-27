package com.jpdr.sensorcollector.view

import android.app.Application
import com.jpdr.sensorcollector.R
import com.jpdr.sensorcollector.SensorAnalyzer
import com.jpdr.sensorcollector.manager.FileManager
import com.jpdr.sensorcollector.manager.SensorManager
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private val context: Application = mockk(relaxed = true)
    private val sensorManager: SensorManager = mockk()
    private val fileManager: FileManager = mockk()
    private val sensorAnalyzer: SensorAnalyzer = mockk()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel(
            context = context,
            sensorManager = sensorManager,
            fileManager = fileManager,
            sensorAnalyzer = sensorAnalyzer
        )

        justRun { fileManager.createDataFileIfNeeded(any()) }
        justRun { fileManager.writeToFile(any(), any(), any(), any(), any()) }
        justRun { fileManager.closeFile() }
        justRun { sensorManager.startCollectingData(any()) }
        justRun { sensorManager.stopCollectingData() }
    }

    private val defaultState = SensorCollectorState(
        availableFrequencies = listOf("100Hz", "200Hz", "MAX"),
        selectedFrequency = "100Hz",
    )

    @Test
    fun `state - empty state is emitted`() {
        val initialState = viewModel.state.value

        assertEquals(defaultState, initialState)
    }

    @Test
    fun `handleIntent - UpdateSessionName - state is updated`() {
        val sessionName = "sessionName"
        val intent = MainIntent.UpdateSessionName(sessionName)

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - SelectFrequency - state is updated`() {
        val frequency = "MAX"
        val intent = MainIntent.SelectFrequency(frequency)

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            selectedFrequency = frequency
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - ToggleDropdownMenu - state is updated`() {
        val expanded = true
        val intent = MainIntent.ToggleDropdownMenu(expanded)

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            isDropdownMenuExpanded = expanded
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - DisplaySessionData with empty sessionName - state is updated with error`() {
        val intent = MainIntent.DisplaySessionData

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            errorRes = R.string.error_empty_session_name
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - DisplaySessionData and FileManager returns empty data - state is updated with error`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))

        val intent = MainIntent.DisplaySessionData

        every { fileManager.getSessionData(any()) } returns emptyList()

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            errorRes = R.string.error_empty_session_data
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            fileManager.getSessionData(sessionName)
        }
    }

    @Test
    fun `handleIntent - DisplaySessionData and FileManager returns data - state is updated sessionData`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))

        val intent = MainIntent.DisplaySessionData

        val sessionData = listOf(listOf(""))

        every { fileManager.getSessionData(any()) } returns sessionData

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            sessionData = sessionData
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            fileManager.getSessionData(sessionName)
        }
    }

    @Test
    fun `handleIntent - DisplayLastReportData with empty sessionName - state is updated with error`() {
        val intent = MainIntent.DisplayLastReportData

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            errorRes = R.string.error_empty_session_name
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - DisplayLastReportData and FileManager returns empty data - state is updated with error`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))

        val intent = MainIntent.DisplayLastReportData

        every { fileManager.getLastReportData(any()) } returns emptyList()

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            errorRes = R.string.error_empty_report_data
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            fileManager.getLastReportData(sessionName)
        }
    }

    @Test
    fun `handleIntent - DisplayLastReportData and FileManager returns data - state is updated sessionData`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))

        val intent = MainIntent.DisplayLastReportData

        val reportData = listOf(listOf(""))

        every { fileManager.getLastReportData(any()) } returns reportData

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            reportData = reportData
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            fileManager.getLastReportData(sessionName)
        }
    }

    @Test
    fun `handleIntent - StartCollecting with empty sessionName - state is updated with error`() {
        val intent = MainIntent.StartCollecting

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            errorRes = R.string.error_empty_session_name
        )
        assertEquals(expected, state)
    }

    @Test
    fun `handleIntent - StartCollecting with sessionName - state is updated`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))

        val intent = MainIntent.StartCollecting

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            isCollecting = true
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            sensorManager.startCollectingData(sessionName)
        }
    }

    @Test
    fun `handleIntent - StopCollecting with sessionName - state is updated`() {
        val sessionName = "sessionName"
        viewModel.handleIntent(MainIntent.UpdateSessionName(sessionName))
        viewModel.handleIntent(MainIntent.StartCollecting)

        val intent = MainIntent.StopCollecting

        viewModel.handleIntent(intent)
        val state = viewModel.state.value

        val expected = defaultState.copy(
            sessionName = sessionName,
            isCollecting = false
        )
        assertEquals(expected, state)

        verify(exactly = 1) {
            sensorManager.stopCollectingData()
        }
    }
}