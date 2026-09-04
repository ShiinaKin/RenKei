package io.sakurasou.renkei.network

import io.sakurasou.renkei.settings.AppSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RenkeiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: RenkeiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = RenkeiClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun registerDeviceUsesBasicAuthenticationAndReturnsToken() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"token":"device-token"}"""),
            )

            val token =
                client.registerDevice(
                    settings = settings(),
                    deviceName = "Pixel",
                    publicKey = "public-key",
                    username = "admin",
                    password = "secret",
                )

            val request = server.takeRequest()
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("device-token", token)
            assertEquals("/device/regist", request.path)
            assertEquals(Credentials.basic("admin", "secret"), request.getHeader("Authorization"))
            assertEquals("Pixel", body.getValue("deviceName").jsonPrimitive.content)
            assertEquals("device-id", body.getValue("uniqueID").jsonPrimitive.content)
            assertEquals("ANDROID", body.getValue("platform").jsonPrimitive.content)
            assertEquals("public-key", body.getValue("publicKey").jsonPrimitive.content)
        }

    @Test
    fun updatePublicKeyUsesBearerToken() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(204))

            client.updatePublicKey(settings(), "device-token", "new-public-key")

            val request = server.takeRequest()
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("PATCH", request.method)
            assertEquals("/device/public-key", request.path)
            assertEquals("Bearer device-token", request.getHeader("Authorization"))
            assertEquals("new-public-key", body.getValue("publicKey").jsonPrimitive.content)
        }

    @Test
    fun sendTestMessageUsesBearerToken() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("decrypted-content"))

            val response = client.sendTestMessage(settings(), "device-token", "base64-cipher-text")

            val request = server.takeRequest()
            assertEquals("decrypted-content", response)
            assertEquals("/message/send-test", request.path)
            assertEquals("Bearer device-token", request.getHeader("Authorization"))
            assertEquals("text/plain; charset=utf-8", request.getHeader("Content-Type"))
            assertEquals("base64-cipher-text", request.body.readUtf8())
        }

    @Test
    fun sendMessageUsesPureCipherTextBody() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(202).setBody("1"))

            client.sendMessage(settings(), "device-token", "encrypted-event")

            val request = server.takeRequest()
            assertEquals("/message/send", request.path)
            assertEquals("Bearer device-token", request.getHeader("Authorization"))
            assertEquals("text/plain; charset=utf-8", request.getHeader("Content-Type"))
            assertEquals("encrypted-event", request.body.readUtf8())
        }

    private fun settings(): AppSettings {
        val url = server.url("/")
        return AppSettings(
            serverAddress = "${url.scheme}://${url.host}",
            serverPort = url.port,
            identifier = "device-id",
        )
    }
}
