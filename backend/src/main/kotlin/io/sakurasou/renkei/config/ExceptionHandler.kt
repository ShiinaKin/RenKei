package io.sakurasou.renkei.config

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import org.slf4j.LoggerFactory

/**
 * @author ShiinaKin
 * 2024/10/6 17:26
 */

fun StatusPagesConfig.exceptionHandler() {
    exception<Throwable> { call, cause ->
        if (cause.cause is JsonConvertException && cause.message?.contains("io.sakurasou.controller.request") == true) {
            logger.warn(
                "Invalid JSON request: method={}, path={}, callId={}",
                call.request.httpMethod.value,
                call.request.path(),
                call.callId,
            )
            call.respond(HttpStatusCode.BadRequest, "Invalid JSON request")
            return@exception
        }
        logger.error(
            "Unhandled request failure: method={}, path={}, callId={}",
            call.request.httpMethod.value,
            call.request.path(),
            call.callId,
            cause,
        )
        call.respondText(text = "Internal server error", status = HttpStatusCode.InternalServerError)
    }
    exception<RequestValidationException> { call, cause ->
        logger.warn(
            "Request validation failed: method={}, path={}, callId={}",
            call.request.httpMethod.value,
            call.request.path(),
            call.callId,
        )
        call.respond(status = HttpStatusCode.BadRequest, message = cause.message?.substringAfter("Reasons: ") as Any)
    }
}

private val logger = LoggerFactory.getLogger("RequestExceptionHandler")
