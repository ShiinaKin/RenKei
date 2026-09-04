package io.sakurasou.renkei.settings

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsSerializerTest {
    @Test
    fun roundTripPreservesSettings() =
        runBlocking {
            val expected =
                AppSettings(
                    serverAddress = "https://example.com",
                    serverPort = 8443,
                    identifier = "test-device",
                    token = "test-token",
                )
            val output = ByteArrayOutputStream()

            AppSettingsSerializer.writeTo(expected, output)
            val actual =
                AppSettingsSerializer.readFrom(
                    ByteArrayInputStream(output.toByteArray()),
                )

            assertEquals(expected, actual)
        }

    @Test
    fun unknownFieldsAreIgnored() =
        runBlocking {
            val input =
                ByteArrayInputStream(
                    """
                    {
                      "serverAddress": "https://example.com",
                      "serverPort": 443,
                      "identifier": "test-device",
                      "futureField": true
                    }
                    """.trimIndent().encodeToByteArray(),
                )

            val settings = AppSettingsSerializer.readFrom(input)

            assertEquals("https://example.com", settings.serverAddress)
            assertEquals(443, settings.serverPort)
            assertEquals("test-device", settings.identifier)
            assertEquals("", settings.token)
        }
}
