package io.sakurasou.renkei.controller

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.sakurasou.renkei.controller.request.MessageRequest
import io.sakurasou.renkei.crypto.MessageCrypto
import io.sakurasou.renkei.di.inject
import io.sakurasou.renkei.extension.getDeviceUniqueID
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.dao.message.MessageDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO
import io.sakurasou.renkei.model.entity.message.Message
import io.sakurasou.renkei.notification.MessageNotification
import io.sakurasou.renkei.util.logReference
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * @author Shiina Kin
 * 2026/9/2 00:49
 */

fun Route.messageRoute() {
    val deviceDAO by inject<DeviceDAO>()
    val messageDAO by inject<MessageDAO>()
    val subscribeRelationDAO by inject<SubcribeRelationDAO>()
    val messageNotification by inject<MessageNotification>()
    route("message") {
        sendMessageTest(deviceDAO)
        sendMessage(deviceDAO, messageDAO, messageNotification)
        getMessage(messageDAO, subscribeRelationDAO)
    }
}

private fun Route.sendMessageTest(deviceDAO: DeviceDAO) {
    route("send-test") {
        post {
            val content = call.receiveDecryptedContent(deviceDAO) ?: return@post
            logger.info(
                "Validated test message: deviceRef={}, contentBytes={}",
                logReference(call.getDeviceUniqueID()),
                content.toByteArray(Charsets.UTF_8).size,
            )
            call.respondText(content, ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}

private fun Route.sendMessage(
    deviceDAO: DeviceDAO,
    messageDAO: MessageDAO,
    messageNotification: MessageNotification,
) {
    route("send") {
        post {
            val decryptedContent = call.receiveDecryptedContent(deviceDAO) ?: return@post
            val messageRequest =
                runCatching { Json.decodeFromString<MessageRequest>(decryptedContent) }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, "Invalid message payload")
                        return@post
                    }
            val deviceUniqueID = call.getDeviceUniqueID()
            val title = messageRequest.title
            val content = messageRequest.content
            val previewContent = content.take(MESSAGE_PREVIEW_LENGTH)
            val messageID =
                messageDAO
                    .saveMessage(
                        Message(
                            deviceUniqueID = deviceUniqueID,
                            title = title,
                            previewContent = previewContent,
                            content = content,
                            timestamp = System.currentTimeMillis(),
                        ),
                    ).also {
                        messageNotification.notify(deviceUniqueID, it, title, content)
                    }
            logger.info(
                "Stored message and scheduled notifications: messageId={}, publisherRef={}, contentBytes={}",
                messageID,
                logReference(deviceUniqueID),
                content.toByteArray(Charsets.UTF_8).size,
            )
            call.respond(HttpStatusCode.Accepted, messageID)
        }
    }
}

private suspend fun ApplicationCall.receiveDecryptedContent(deviceDAO: DeviceDAO): String? {
    val device =
        deviceDAO.getDeviceByUniqueID(getDeviceUniqueID()) ?: run {
            respond(HttpStatusCode.NotFound, "Device not found")
            return null
        }
    return MessageCrypto.decryptWithPublicKey(receiveText(), device.publicKey)
}

private const val MESSAGE_PREVIEW_LENGTH = 30

private fun Route.getMessage(
    messageDAO: MessageDAO,
    subscribeRelationDAO: SubcribeRelationDAO,
) {
    get("{message_id}") {
        val messageID =
            call.pathParameters["message_id"]?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or illegal message id")
                return@get
            }
        val message =
            messageDAO.getMessageById(messageID) ?: run {
                call.respond(HttpStatusCode.NotFound, "Message not found")
                return@get
            }
        val requesterDeviceID = call.getDeviceUniqueID()
        val canRead =
            requesterDeviceID == message.deviceUniqueID ||
                subscribeRelationDAO.isSubscribed(message.deviceUniqueID, requesterDeviceID)
        if (!canRead) {
            logger.warn(
                "Denied message read: messageId={}, requesterRef={}",
                messageID,
                logReference(requesterDeviceID),
            )
            call.respond(HttpStatusCode.Forbidden, "Device is not subscribed to this message publisher")
            return@get
        }
        logger.info(
            "Returned message: messageId={}, requesterRef={}",
            messageID,
            logReference(requesterDeviceID),
        )
        call.respond(HttpStatusCode.OK, message)
    }
}

private val logger = LoggerFactory.getLogger("MessageController")
