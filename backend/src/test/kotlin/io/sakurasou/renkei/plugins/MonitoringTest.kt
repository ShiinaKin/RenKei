package io.sakurasou.renkei.plugins

import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MonitoringTest {
    @Test
    fun `generates and returns a call id`() =
        testApplication {
            application {
                configureMonitoring()
                routing { get("health") { call.respond(HttpStatusCode.OK) } }
            }

            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(16, assertNotNull(response.headers[HttpHeaders.XRequestId]).length)
        }

    @Test
    fun `redacts identifiers from request paths`() {
        assertEquals(
            "/subscription/{publisher_device_id}",
            sanitizedPathForLog("/subscription/device-secret"),
        )
        assertEquals(
            "/device/unregist/{unique_id}",
            sanitizedPathForLog("/device/unregist/device-secret"),
        )
    }
}
