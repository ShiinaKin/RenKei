package io.sakurasou.renkei.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.sakurasou.renkei.controller.request.BarkTargetRequest
import io.sakurasou.renkei.extension.getDeviceUniqueID
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.dao.notification.NotificationTargetDAO
import io.sakurasou.renkei.model.entity.device.DevicePlatform
import io.sakurasou.renkei.model.entity.notification.NotificationProvider
import io.sakurasou.renkei.util.logReference
import org.slf4j.LoggerFactory

fun Route.notificationTargetRoutes(
    notificationTargetDAO: NotificationTargetDAO,
    deviceDAO: DeviceDAO,
) {
    route("notification-target/bark") {
        put {
            val deviceKey = call.receive<BarkTargetRequest>().deviceKey.trim()
            if (!BARK_DEVICE_KEY_REGEX.matches(deviceKey)) {
                logger.warn("Rejected invalid Bark device key")
                call.respond(HttpStatusCode.BadRequest, "Invalid Bark device key")
                return@put
            }

            val deviceUniqueID = call.getDeviceUniqueID()
            val device = deviceDAO.getDeviceByUniqueID(deviceUniqueID)
            if (device?.platform != DevicePlatform.IOS) {
                logger.warn(
                    "Rejected Bark target for non-iOS device: deviceRef={}",
                    logReference(deviceUniqueID),
                )
                call.respond(HttpStatusCode.Forbidden, "Only iOS devices can register a Bark target")
                return@put
            }

            notificationTargetDAO.upsert(deviceUniqueID, NotificationProvider.BARK, deviceKey)
            logger.info("Registered Bark target: deviceRef={}", logReference(deviceUniqueID))
            call.respond(HttpStatusCode.NoContent)
        }

        delete {
            val deviceUniqueID = call.getDeviceUniqueID()
            val deleted =
                notificationTargetDAO.delete(
                    deviceUniqueID,
                    NotificationProvider.BARK,
                )
            logger.info(
                "Bark target removal completed: deviceRef={}, existed={}",
                logReference(deviceUniqueID),
                deleted,
            )
            call.respond(if (deleted) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
        }
    }
}

private val BARK_DEVICE_KEY_REGEX = Regex("^[A-Za-z0-9_-]{8,128}$")
private val logger = LoggerFactory.getLogger("NotificationTargetController")
