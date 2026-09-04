package io.sakurasou.renkei.notification

import io.sakurasou.renkei.config.BarkConfig
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.notification.NotificationTargetDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO
import io.sakurasou.renkei.model.entity.device.DevicePlatform
import io.sakurasou.renkei.model.entity.notification.NotificationProvider
import io.sakurasou.renkei.notification.bark.BarkNotifier
import io.sakurasou.renkei.notification.bark.BarkNotification
import io.sakurasou.renkei.notification.bark.BarkPushResult
import io.sakurasou.renkei.util.logReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class MessageNotification(
    private val subscribeRelationDAO: SubcribeRelationDAO,
    private val notificationTargetDAO: NotificationTargetDAO,
    private val barkNotifier: BarkNotifier,
    private val messageAccessTokenDAO: MessageAccessTokenDAO? = null,
    private val barkConfig: BarkConfig? = null,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(MessageNotification::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun notify(
        messageFromDeviceID: String,
        messageID: Long,
        title: String,
        content: String,
    ): Job =
        scope.launch {
            runCatching { notifySubscribers(messageFromDeviceID, messageID, title, content) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error("Failed to dispatch notification for message {}", messageID, error)
                }
        }

    private suspend fun notifySubscribers(
        messageFromDeviceID: String,
        messageID: Long,
        title: String,
        content: String,
    ) = supervisorScope {
        val subscribers =
            subscribeRelationDAO
                .getSubcribers(messageFromDeviceID)
                .filter { (_, platform) -> platform == DevicePlatform.IOS }
        logger.info(
            "Dispatching Bark notification: messageId={}, publisherRef={}, subscriberCount={}",
            messageID,
            logReference(messageFromDeviceID),
            subscribers.size,
        )
        subscribers.forEach { (subscriberDeviceID, _) ->
            launch {
                runCatching { notifyBarkTarget(subscriberDeviceID, messageID, title, content) }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        logger.error(
                            "Failed to notify Bark target: subscriberRef={}, messageId={}",
                            logReference(subscriberDeviceID),
                            messageID,
                            error,
                        )
                    }
            }
        }
    }

    private suspend fun notifyBarkTarget(
        subscriberDeviceID: String,
        messageID: Long,
        title: String,
        content: String,
    ) {
        val deviceKey =
            notificationTargetDAO.getTarget(subscriberDeviceID, NotificationProvider.BARK)
                ?: run {
                    logger.warn("No Bark target registered: subscriberRef={}", logReference(subscriberDeviceID))
                    return
                }

        val notification = buildNotification(subscriberDeviceID, messageID, title, content)

        var attempt = 1
        while (true) {
            val result =
                runCatching { barkNotifier.notifyNewMessage(deviceKey, messageID, notification) }
                    .getOrElse { error ->
                        if (error is CancellationException) throw error
                        if (attempt >= MAX_ATTEMPTS) throw error
                        logger.warn(
                            "Bark transport failed: subscriberRef={}, messageId={}, attempt={}",
                            logReference(subscriberDeviceID),
                            messageID,
                            attempt,
                            error,
                        )
                        delay(retryDelayMillis(attempt++).milliseconds)
                        continue
                    }

            if (result is BarkPushResult.Rejected && result.isRetryable && attempt < MAX_ATTEMPTS) {
                logger.warn(
                    "Bark temporarily rejected message: messageId={}, subscriberRef={}, httpStatus={}, code={}, attempt={}",
                    messageID,
                    logReference(subscriberDeviceID),
                    result.httpStatus,
                    result.code,
                    attempt,
                )
                delay(retryDelayMillis(attempt++).milliseconds)
                continue
            }

            if (result == BarkPushResult.Disabled) {
                logger.debug(
                    "Bark is disabled; skipped message: messageId={}, subscriberRef={}",
                    messageID,
                    logReference(subscriberDeviceID),
                )
            }
            return
        }
    }

    private suspend fun buildNotification(
        subscriberDeviceID: String,
        messageID: Long,
        title: String,
        content: String,
    ): BarkNotification {
        if (Json.encodeToString(content).toByteArray(Charsets.UTF_8).size <= MAX_INLINE_CONTENT_BYTES) {
            return BarkNotification(body = content, title = title)
        }

        val config =
            barkConfig
                ?: return BarkNotification(body = utf8Prefix(content, MAX_INLINE_CONTENT_BYTES), title = title)
        val tokenDAO =
            messageAccessTokenDAO
                ?: return BarkNotification(body = utf8Prefix(content, MAX_INLINE_CONTENT_BYTES), title = title)
        val expiresAt = Math.addExact(System.currentTimeMillis(), config.messageLinkTTL.toMillis())
        val token = tokenDAO.issue(messageID, subscriberDeviceID, expiresAt)
        logger.info(
            "Issued one-time long-message link: messageId={}, subscriberRef={}, expiresAt={}",
            messageID,
            logReference(subscriberDeviceID),
            expiresAt,
        )
        return BarkNotification(
            body = utf8Prefix(content, LONG_MESSAGE_PREVIEW_BYTES) + "\n\n点击查看并复制全文",
            url = config.messageURL(token).toString(),
            title = title,
        )
    }

    private fun utf8Prefix(
        value: String,
        maxBytes: Int,
    ): String {
        var usedBytes = 0
        var endIndex = 0
        while (endIndex < value.length) {
            val codePoint = value.codePointAt(endIndex)
            val characterBytes = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            if (usedBytes + characterBytes > maxBytes) break
            usedBytes += characterBytes
            endIndex += Character.charCount(codePoint)
        }
        return value.substring(0, endIndex)
    }

    override fun close() {
        scope.cancel()
    }

    private fun retryDelayMillis(attempt: Int): Long = BASE_RETRY_DELAY_MILLIS shl (attempt - 1)

    private companion object {
        const val MAX_INLINE_CONTENT_BYTES = 2 * 1024
        const val LONG_MESSAGE_PREVIEW_BYTES = 180
        const val MAX_ATTEMPTS = 3
        const val BASE_RETRY_DELAY_MILLIS = 250L
    }
}
