package com.jpdr.sensorcollector.util

import com.jpdr.sensorcollector.util.FrequencyCalculator.hertzToMicroseconds
import junit.framework.TestCase.assertEquals
import org.junit.Test

class FrequencyCalculatorTest {

    @Test
    fun `hertzToMicroseconds - 100hz - returns 10_000 microseconds`() {
        assertEquals(10_000, hertzToMicroseconds(100))
    }

    @Test
    fun `hertzToMicroseconds - 200hz - returns 5000 microseconds`() {
        assertEquals(5_000, hertzToMicroseconds(200))
    }
}