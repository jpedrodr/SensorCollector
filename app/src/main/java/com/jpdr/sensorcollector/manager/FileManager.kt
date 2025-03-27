package com.jpdr.sensorcollector.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var fileWriter: FileWriter? = null

    /**
     * Creates a data file in the given [sessionName] directory if it doesn't exist
     * If it didn't exist of was empty, add the headers
     */
    fun createDataFileIfNeeded(sessionName: String) {
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
    }

    fun writeToFile(timestamp: Long, sensorId: Int, x: Float, y: Float, z: Float) {
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

    fun closeFile() {
        try {
            fileWriter?.flush()
            fileWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
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

    /**
     * Returns the data of the last report of the given [sessionName]
     * Returns empty if it doesn't exist
     */
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