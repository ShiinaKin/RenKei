package io.sakurasou.renkei.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            CALL_ID_REGEX.matches(callId)
        }
        generate(length = 16)
    }
    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        mdc("callId") { call -> call.callId }
        format { call ->
            val status = call.response.status()?.value ?: 0
            val path = sanitizedPathForLog(call.request.path())
            "HTTP ${call.request.httpMethod.value} $path status=$status durationMs=${call.processingTimeMillis()}"
        }
    }
}

internal fun sanitizedPathForLog(path: String): String =
    when {
        path.startsWith("/subscription/") -> "/subscription/{publisher_device_id}"
        path.startsWith("/device/unregist/") -> "/device/unregist/{unique_id}"
        else -> path.take(MAX_LOGGED_PATH_LENGTH)
    }

private val CALL_ID_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")
private const val MAX_LOGGED_PATH_LENGTH = 200
