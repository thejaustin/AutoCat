package app.lawnchair.categorization.llm.test

import app.lawnchair.categorization.llm.LLMLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LLMLoggerTest {

    @Before
    fun setup() {
        LLMLogger.clearLogs()
    }

    @Test
    fun `logRequest adds entry to buffer and emits to flow`() = runTest {
        val provider = "test_provider"
        val endpoint = "https://api.example.com"

        LLMLogger.logRequest(provider, endpoint)

        val logs = LLMLogger.getRecentLogs()
        assertEquals(1, logs.size)
        assertEquals(provider, logs[0].provider)
        assertEquals("API_REQUEST", logs[0].operation)
        assertTrue(logs[0].message.contains(endpoint))

        // Check flow emission
        val emittedLog = LLMLogger.logFlow.first()
        assertEquals(provider, emittedLog.provider)
    }

    @Test
    fun `logResponse handles success and error status codes`() {
        LLMLogger.logResponse("provider1", 200, "OK", 100L)
        LLMLogger.logResponse("provider2", 401, "Unauthorized", 50L)

        val logs = LLMLogger.getRecentLogs()
        assertEquals(2, logs.size)
        assertEquals(LLMLogger.LogLevel.INFO, logs[0].level)
        assertEquals(LLMLogger.LogLevel.ERROR, logs[1].level)
    }

    @Test
    fun `logError captures exception details`() {
        val exception = RuntimeException("Test failure")
        LLMLogger.logError("test_provider", "TEST_OP", exception)

        val logs = LLMLogger.getRecentLogs()
        assertEquals(1, logs.size)
        assertEquals(LLMLogger.LogLevel.ERROR, logs[0].level)
        assertEquals("Test failure", logs[0].message)
        assertEquals(exception, logs[0].exception)
    }

    @Test
    fun `log buffer maintains maximum entry limit`() {
        // Log more than MAX_LOG_ENTRIES
        repeat(250) {
            LLMLogger.logInfo("test", "OP", "Message $it")
        }

        val logs = LLMLogger.getRecentLogs()
        assertEquals(200, logs.size) // MAX_LOG_ENTRIES is 200
        assertEquals("Message 249", logs.last().message)
    }

    @Test
    fun `getStatistics returns correct counts`() {
        LLMLogger.logInfo("p1", "OP", "msg")
        LLMLogger.logError("p1", "OP", RuntimeException("err"))
        LLMLogger.logWarning("p2", "OP", "warn")

        val stats = LLMLogger.getStatistics()
        assertEquals(3, stats.totalEntries)
        assertEquals(1, stats.infoCount)
        assertEquals(1, stats.errorCount)
        assertEquals(1, stats.warningCount)
        assertEquals(2, stats.providerCounts["p1"])
        assertEquals(1, stats.providerCounts["p2"])
    }
}
