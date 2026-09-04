package io.sakurasou.renkei.extension

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun ApplicationCall.getDeviceUniqueID(): String {
    val principal = principal<JWTPrincipal>() ?: throw RuntimeException()
    val deviceUniqueID = principal.payload.getClaim("device_id").asString()
    return deviceUniqueID
}
