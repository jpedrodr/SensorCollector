package com.jpdr.sensorcollector

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

class SensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager by lazy { context.getSystemService(SENSOR_SERVICE) as SensorManager }
    private val accelerometerSensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    private var fileWriter: FileWriter? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val timestamp = System.nanoTime()
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                writeToCSV(timestamp, it.sensor.id, x, y, z)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Do nothing since we don't need to handle accuracy changes
    }

    fun startCollectingData(sessionName: String) {
        val dataFile = getDataFile(sessionName)

        try {
            fileWriter = FileWriter(dataFile, true)

            // file is empty, add headers before writing to it
            if (dataFile.length() == 0L) {
                fileWriter?.append(
                    "$TIMESTAMP_HEADER$VALUE_SEPARATOR$SENSOR_ID_HEADER" +
                            "$VALUE_SEPARATOR$SENSOR_VALUE_X_HEADER$VALUE_SEPARATOR" +
                            "$SENSOR_VALUE_Y_HEADER$VALUE_SEPARATOR$SENSOR_VALUE_Z_HEADER\n"
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopCollectingData() {
        sensorManager.unregisterListener(this)

        try {
            fileWriter?.flush()
            fileWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeToCSV(timestamp: Long, sensorId: Int, x: Float, y: Float, z: Float) {
        try {
            fileWriter
                ?.append(
                    "$timestamp$VALUE_SEPARATOR$sensorId" +
                            "$VALUE_SEPARATOR$x$VALUE_SEPARATOR$y$VALUE_SEPARATOR$z\n"
                )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Gets the session directory based on the [sessionName]
     * If it doesn't exist, creates and returns it
     */
    private fun getSessionDirectory(sessionName: String): File {
        val rootDir = File(context.getExternalFilesDir(null), ROOT_PATH)

        val sessionDir = File(rootDir, sessionName)

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    /**
     * Gets the data file if the given session
     * If it doesn't exist, creates and returns it
     */
    private fun getDataFile(sessionName: String): File {
        val sessionDir = getSessionDirectory(sessionName)

        val dataDir = File(sessionDir, DATA_DIRECTORY)
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        val reportDir = File(sessionDir, REPORT_DIRECTORY)
        if (!reportDir.exists()) {
            reportDir.mkdirs()
        }

        // gets the current data file, or creates a new one if it doesn't exist
        val dataFile =
            dataDir.listFiles { it.isFile && it.name == DATA_FILE_NAME }?.firstOrNull()
                ?: File(dataDir, DATA_FILE_NAME)

        return dataFile
    }

    fun getLastReportData(sessionName: String): List<List<String>> {
        val rootDir = File(context.getExternalFilesDir(null), ROOT_PATH)

        val sessionDir = File(rootDir, sessionName)

        val reportDir = File(sessionDir, REPORT_DIRECTORY)

        val reportFiles = reportDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(REPORT_FILE_NAME_PREFIX) }

        val reportFile = reportFiles?.lastOrNull() ?: return emptyList()

        val data = mutableListOf<List<String>>()

        try {
            BufferedReader(FileReader(reportFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Split by commas and add to the list
                    line?.split(VALUE_SEPARATOR)?.let {
                        data.add(it)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return data.toList()
    }

    /**
     * Gets the session data based on the [sessionName]
     * Returns empty if it doesn't exist
     */
    fun getSessionData(sessionName: String): List<List<String>> {
        val rootDir = File(context.getExternalFilesDir(null), ROOT_PATH)

        val sessionDir = File(rootDir, sessionName)

        val dataDir = File(sessionDir, DATA_DIRECTORY)

        val dataFile = File(dataDir, DATA_FILE_NAME)

        val data = mutableListOf<List<String>>()

        try {
            BufferedReader(FileReader(dataFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Split by commas and add to the list
                    line?.split(VALUE_SEPARATOR)?.let {
                        data.add(it)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return data.toList()
    }

    companion object {
        private const val ROOT_PATH = "/SensorCollector/collects"
        private const val DATA_DIRECTORY = "data"
        private const val REPORT_DIRECTORY = "report"
        private const val REPORT_FILE_NAME_PREFIX = "accelerometer_report_"
        private const val DATA_FILE_NAME = "accelerometer.csv"

        private const val TIMESTAMP_HEADER = "timestamp (ns)"
        private const val SENSOR_ID_HEADER = "sensor_id"
        private const val SENSOR_VALUE_X_HEADER = "sensor_value_x (m/s2)"
        private const val SENSOR_VALUE_Y_HEADER = "sensor_value_y (m/s2)"
        private const val SENSOR_VALUE_Z_HEADER = "sensor_value_z (m/s2)"
        private const val VALUE_SEPARATOR = ","
    }
}