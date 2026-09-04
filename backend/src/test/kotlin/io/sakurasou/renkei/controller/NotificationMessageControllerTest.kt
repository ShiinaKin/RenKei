package io.sakurasou.renkei.controller

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.message.RedeemedMessage
import io.sakurasou.renkei.plugins.configureSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationMessageControllerTest {
    @Test
    fun `serves the token-free page and redeems a token once`() =
        testApplication {
            val tokens = FakeTokens()
            application {
                configureSerialization()
                routing { notificationMessageRoutes(tokens) }
            }

            val page = client.get("/notification-message")
            assertEquals(HttpStatusCode.OK, page.status)
            assertEquals("no-store", page.headers["Cache-Control"])
            assertFalse(page.headers.toString().contains(VALID_TOKEN))

            val first =
                client.post("/notification-message/redeem") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("""{"token":"$VALID_TOKEN"}""")
                }
            val second =
                client.post("/notification-message/redeem") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("""{"token":"$VALID_TOKEN"}""")
                }

            assertEquals(HttpStatusCode.OK, first.status)
            assertTrue(first.bodyAsText().contains("full message"))
            assertEquals(HttpStatusCode.Gone, second.status)
        }

    private class FakeTokens : MessageAccessTokenDAO {
        private var consumed = false

        override suspend fun issue(
            messageID: Long,
            subscriberDeviceID: String,
            expiresAt: Long,
        ): String = error("Not used")

        override suspend fun consume(
            token: String,
            now: Long,
        ): RedeemedMessage? {
            if (token != VALID_TOKEN || consumed) return null
            consumed = true
            return RedeemedMessage(42, "full message")
        }
    }

    private companion object {
        val VALID_TOKEN = "a".repeat(43)
    }
}
