package com.buspay.app.device

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoStopRequestInputTest {
    @Test
    fun `trigger delivers request only while listener is active`() {
        val input = DemoStopRequestInput()
        var requestCount = 0

        input.trigger()
        input.start { requestCount += 1 }
        input.trigger()
        input.stop()
        input.trigger()

        assertEquals(1, requestCount)
    }
}
