package io.sakurasou.renkei.notification.bark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BarkPushPayload(
    @SerialName("device_key")
    val deviceKey: String,
    val body: String,
    val ciphertext: String,
    val iv: String,
)

@Serializable
internal data class BarkEncryptedPayload(
    val title: String,
    val body: String,
    val group: String,
    val id: String,
    val url: String? = null,
    val isArchive: String = "1",
)

@Serializable
internal data class BarkResponse(
    val code: Int,
    val message: String,
    val timestamp: Long? = null,
)

sealed interface BarkPushResult {
    data class Accepted(
        val timestamp: Long?,
        val code: Int = 200,
    ) : BarkPushResult

    data class Rejected(
        val httpStatus: Int,
        val code: Int?,
        val message: String,
    ) : BarkPushResult {
        val isRetryable: Boolean
            get() = httpStatus == 429 || httpStatus >= 500 || code == 429 || (code != null && code >= 500)
    }

    data object Disabled : BarkPushResult
}

interface BarkNotifier {
    suspend fun notifyNewMessage(
        deviceKey: String,
        messageID: Long,
        message: BarkNotification,
    ): BarkPushResult
}

data class BarkNotification(
    val body: String,
    val url: String? = null,
    val title: String? = null,
)
