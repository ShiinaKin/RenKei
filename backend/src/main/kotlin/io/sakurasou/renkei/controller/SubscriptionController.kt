package io.sakurasou.renkei.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.sakurasou.renkei.extension.getDeviceUniqueID
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO
import io.sakurasou.renkei.util.logReference
import org.slf4j.LoggerFactory

fun Route.subscriptionRoutes(
    subscribeRelationDAO: SubcribeRelationDAO,
    deviceDAO: DeviceDAO,
) {
    route("subscription/{publisher_device_id}") {
        post {
            val publisherDeviceID =
                call.pathParameters["publisher_device_id"]?.trim()?.takeIf(String::isNotEmpty) ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing publisher device ID")
                    return@post
                }
            val subscriberDeviceID = call.getDeviceUniqueID()
            if (publisherDeviceID == subscriberDeviceID) {
                call.respond(HttpStatusCode.BadRequest, "A device cannot subscribe to itself")
                return@post
            }
            if (deviceDAO.getDeviceByUniqueID(publisherDeviceID) == null) {
                call.respond(HttpStatusCode.NotFound, "Publisher device not found")
                return@post
            }

            val created = subscribeRelationDAO.subscribe(publisherDeviceID, subscriberDeviceID)
            logger.info(
                "Subscription {}: publisherRef={}, subscriberRef={}",
                if (created) "created" else "already existed",
                logReference(publisherDeviceID),
                logReference(subscriberDeviceID),
            )
            call.respond(if (created) HttpStatusCode.Created else HttpStatusCode.NoContent)
        }

        delete {
            val publisherDeviceID =
                call.pathParameters["publisher_device_id"]?.trim()?.takeIf(String::isNotEmpty) ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing publisher device ID")
                    return@delete
                }
            val subscriberDeviceID = call.getDeviceUniqueID()
            val deleted = subscribeRelationDAO.unsubscribe(publisherDeviceID, subscriberDeviceID)
            logger.info(
                "Subscription removal completed: publisherRef={}, subscriberRef={}, existed={}",
                logReference(publisherDeviceID),
                logReference(subscriberDeviceID),
                deleted,
            )
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private val logger = LoggerFactory.getLogger("SubscriptionController")
