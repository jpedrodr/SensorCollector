package com.jpdr.sensorcollector.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

class SensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) : SensorEventListener {

    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val accelerometerSensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val timestamp = System.nanoTime()
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                fileManager.writeToFile(timestamp, it.sensor.id, x, y, z)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Do nothing since we don't need to handle accuracy changes
    }

    fun startCollectingData(sessionName: String) {
        fileManager.createDataFileIfNeeded(sessionName)
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopCollectingData() {
        sensorManager.unregisterListener(this)
        fileManager.closeFile()
    }
}