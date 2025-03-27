package com.jpdr.sensorcollector.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.jpdr.sensorcollector.util.FrequencyCalculator.hertzToMicroseconds
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun startCollectingData(sessionName: String, frequency: SensorFrequency) {
        fileManager.createDataFileIfNeeded(sessionName)

        val delayInMicroseconds = frequency.toDelayInMicroseconds()

        // register the accelerometer with the request frequency
        sensorManager.registerListener(this, accelerometerSensor, delayInMicroseconds)
    }

    fun stopCollectingData() {
        sensorManager.unregisterListener(this)
        fileManager.closeFile()
    }

    /**
     * The maximum delay (in microseconds) between readings
     */
    fun getMaxSensorDelayInMicroseconds(): Int = accelerometerSensor?.maxDelay ?: 1

    // map the app Frequency model to delays in microseconds
    private fun SensorFrequency.toDelayInMicroseconds() = when (this) {
        SensorFrequency.ONE_HUNDRED -> hertzToMicroseconds(100) // 100Hz = 10 microseconds delay between readings
        SensorFrequency.TWO_HUNDRED -> hertzToMicroseconds(200) // 200Hz = 5 microseconds delay between readings
        SensorFrequency.MAX -> SensorManager.SENSOR_DELAY_FASTEST // 0ms, reads as fast as possible
    }

    enum class SensorFrequency {
        ONE_HUNDRED, // 100Hz
        TWO_HUNDRED, // 200Hz
        MAX // MAX frequency
    }
}