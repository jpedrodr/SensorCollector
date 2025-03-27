package com.jpdr.sensorcollector.util

object FrequencyCalculator {
    /**
     * Converts hertz to microseconds
     * 1 seconds = 1_000_000 microseconds
     * 1 hertz = once per second = once per 1_000_000 microseconds
     */
    fun hertzToMicroseconds(hertz: Int): Int  = 1_000_000/hertz
}