package io.sakurasou.renkei.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.sakurasou.renkei.controller.request.DevicePublicKeyUpdateRequest
import io.sakurasou.renkei.controller.request.DeviceRegistRequest
import io.sakurasou.renkei.controller.response.DeviceRegistrationResponse
import io.sakurasou.renkei.db.DatabaseSingleton.dbQuery
import io.sakurasou.renkei.di.inject
import io.sakurasou.renkei.extension.getDeviceUniqueID
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.entity.device.Device
import io.sakurasou.renkei.util.JwtUtils
import io.sakurasou.renkei.util.logReference
import org.slf4j.LoggerFactory

/**
 * @author Shiina Kin
 * 2026/9/1 17:15
 */
fun Route.deviceRoutes() {
    val deviceDAO by inject<DeviceDAO>()
    route("device") {
        registDevice(deviceDAO)
        unregistDevice(deviceDAO)
    }
}

fun Route.authenticatedDeviceRoutes() {
    val deviceDAO by inject<DeviceDAO>()
    route("device") {
        updatePublicKey(deviceDAO)
    }
}

private fun Route.registDevice(deviceDAO: DeviceDAO) {
    route("regist") {
        install(RequestValidation) {
            validate<DeviceRegistRequest> { request ->
                if (request.deviceName.isBlank()) {
                    ValidationResult.Invalid("deviceName is required")
                } else if (request.uniqueID.isBlank()) {
                    ValidationResult.Invalid("uniqueID is required")
                } else if (request.publicKey.isBlank()) {
                    ValidationResult.Invalid("publicKey is required")
                } else {
                    ValidationResult.Valid
                }
            }
        }
        post {
            val request = call.receive<DeviceRegistRequest>()
            val token =
                dbQuery {
                    deviceDAO.saveDevice(
                        Device(
                            name = request.deviceName,
                            uniqueID = request.uniqueID,
                            platform = request.platform,
                            publicKey = request.publicKey,
                        ),
                    )
                    JwtUtils.generateJwtToken(request.uniqueID)
                }
            logger.info(
                "Registered device: deviceRef={}, platform={}",
                logReference(request.uniqueID),
                request.platform,
            )
            call.respond(HttpStatusCode.Created, DeviceRegistrationResponse(token))
        }
    }
}

private fun Route.updatePublicKey(deviceDAO: DeviceDAO) {
    route("public-key") {
        install(RequestValidation) {
            validate<DevicePublicKeyUpdateRequest> { request ->
                if (request.publicKey.isBlank()) {
                    ValidationResult.Invalid("publicKey is required")
                } else {
                    ValidationResult.Valid
                }
            }
        }
        patch {
            val request = call.receive<DevicePublicKeyUpdateRequest>()
            val deviceUniqueID = call.getDeviceUniqueID()
            val updated = deviceDAO.updatePublicKey(deviceUniqueID, request.publicKey)
            if (updated) {
                logger.info("Updated device public key: deviceRef={}", logReference(deviceUniqueID))
                call.respond(HttpStatusCode.NoContent)
            } else {
                logger.warn("Public-key update target was not found: deviceRef={}", logReference(deviceUniqueID))
                call.respond(HttpStatusCode.NotFound, "Device not found")
            }
        }
    }
}

private fun Route.unregistDevice(deviceDAO: DeviceDAO) {
    route("unregist/{unique_id}") {
        patch {
            val uniqueID =
                call.pathParameters["unique_id"]?.takeIf { it.isNotBlank() } ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing unique id")
                    return@patch
                }
            val result = deviceDAO.deleteDeviceByUniqueID(uniqueID)
            if (!result) {
                logger.warn("Device removal target was not found: deviceRef={}", logReference(uniqueID))
                call.respond(HttpStatusCode.NotFound)
            } else {
                logger.info("Removed device: deviceRef={}", logReference(uniqueID))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private val logger = LoggerFactory.getLogger("DeviceController")
