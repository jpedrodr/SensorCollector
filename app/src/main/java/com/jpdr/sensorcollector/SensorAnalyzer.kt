package com.jpdr.sensorcollector

import javax.inject.Inject

/**
 * Wrapper class to load and call the C++ library
 */
class SensorAnalyzer @Inject constructor() {

    init {
        System.loadLibrary("sensor_analyzer")
    }

    external fun createReport(sessionName: String)
}