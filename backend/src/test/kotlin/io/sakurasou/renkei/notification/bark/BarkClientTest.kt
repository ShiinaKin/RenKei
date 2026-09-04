package io.sakurasou.renkei.notification.bark

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.sakurasou.renkei.config.BarkConfig
import java.net.URI
import java.time.Duration
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class BarkClientTest {
    private val encryptionKey = "12345678901234567890123456789012"
    private val config =
        BarkConfig(
            baseURL = URI.create("https://api.day.app"),
            publicBaseURL = URI.create("https://renkei.example"),
            title = "RenKei",
            group = "renkei",
            encryptionKey = encryptionKey,
            requestTimeout = Duration.ofSeconds(10),
            messageLinkTTL = Duration.ofMinutes(10),
        )
    private val client =
        BarkClient(
            config,
            HttpClient(MockEngine { error("No HTTP request expected") }),
        )

    @Test
    fun `builds an encrypted Bark v2 payload`() {
        val payload =
            client.buildPayload(
                "abcDEF123",
                42,
                BarkNotification(
                    body = "message body",
                    url = "https://renkei.example/notification-message#token",
                    title = "10086",
                ),
            )
        val encryptedPayload = decrypt(payload)

        assertEquals("abcDEF123", payload.deviceKey)
        assertEquals("Encrypted message", payload.body)
        assertEquals(12, payload.iv.length)
        assertEquals("10086", encryptedPayload.title)
        assertEquals("message body", encryptedPayload.body)
        assertEquals("renkei", encryptedPayload.group)
        assertEquals("renkei-message-42", encryptedPayload.id)
        assertEquals("https://renkei.example/notification-message#token", encryptedPayload.url)
        assertEquals("1", encryptedPayload.isArchive)
    }

    @Test
    fun `posts with Ktor Client and evaluates the Bark response`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    assertEquals("https://api.day.app/push", request.url.toString())
                    assertEquals(ContentType.Application.Json, request.body.contentType)
                    assertEquals(ContentType.Application.Json.toString(), request.headers[HttpHeaders.Accept])
                    respond(
                        content = """{"code":200,"message":"success","timestamp":42}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val ktorClient = HttpClient(engine)
            val barkClient = BarkClient(config, ktorClient)

            val result = barkClient.notifyNewMessage("abcDEF123", 42, BarkNotification("message body"))

            assertEquals(BarkPushResult.Accepted(42), result)
            barkClient.close()
        }

    @Test
    fun `accepts only a successful HTTP and Bark response`() {
        val accepted = client.parseResponse(200, """{"code":200,"message":"success","timestamp":42}""")
        val businessFailure = client.parseResponse(200, """{"code":500,"message":"push failed"}""")

        assertEquals(BarkPushResult.Accepted(42), accepted)
        assertIs<BarkPushResult.Rejected>(businessFailure)
        assertTrue(businessFailure.isRetryable)
    }

    @Test
    fun `rejects a malformed response`() {
        val result = client.parseResponse(200, "not-json")

        assertEquals(
            BarkPushResult.Rejected(200, null, "Invalid Bark response"),
            result,
        )
    }

    private fun decrypt(payload: BarkPushPayload): BarkEncryptedPayload {
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(encryptionKey.toByteArray(Charsets.UTF_8), "AES"),
                    GCMParameterSpec(128, payload.iv.toByteArray(Charsets.US_ASCII)),
                )
            }
        val json = cipher.doFinal(Base64.getDecoder().decode(payload.ciphertext)).decodeToString()
        return Json.decodeFromString(json)
    }
}
