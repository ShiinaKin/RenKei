package io.sakurasou.renkei.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LogSanitizerTest {
    @Test
    fun `fingerprints identifiers and strips log control characters`() {
        val reference = logReference("device-secret")

        assertEquals(12, reference.length)
        assertFalse(reference.contains("device-secret"))
        assertEquals("line one line two", logText("line one\nline two"))
    }
}
