package io.sakurasou.renkei.notification.bark

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.sakurasou.renkei.config.BarkConfig
import io.sakurasou.renkei.util.logText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class BarkClient(
    private val config: BarkConfig,
    private val httpClient: HttpClient = createHttpClient(config),
    private val json: Json = Json { ignoreUnknownKeys = true },
) :
    BarkNotifier,
    AutoCloseable {
    private val logger = LoggerFactory.getLogger(BarkClient::class.java)
    private val crypto = BarkCrypto(config.encryptionKey)

    override suspend fun notifyNewMessage(
        deviceKey: String,
        messageID: Long,
        message: BarkNotification,
    ): BarkPushResult {
        val payload = json.encodeToString(buildPayload(deviceKey, messageID, message))
        val response =
            httpClient.post(config.pushEndpoint.toString()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                setBody(payload)
            }
        val httpStatus = response.status.value
        val result = parseResponse(httpStatus, response.bodyAsText())
        logResponse(messageID, httpStatus, result)
        return result
    }

    internal fun buildPayload(
        deviceKey: String,
        messageID: Long,
        message: BarkNotification,
    ): BarkPushPayload {
        val encryptedPayload =
            json.encodeToString(
                BarkEncryptedPayload(
                    title = message.title?.takeIf(String::isNotBlank) ?: config.title,
                    body = message.body.ifBlank { DEFAULT_BODY },
                    group = config.group,
                    id = "renkei-message-$messageID",
                    url = message.url,
                ),
            )
        val ciphertext = crypto.encrypt(encryptedPayload)
        return BarkPushPayload(
            deviceKey = deviceKey,
            body = ENCRYPTED_BODY,
            ciphertext = ciphertext.value,
            iv = ciphertext.iv,
        )
    }

    internal fun parseResponse(
        httpStatus: Int,
        body: String,
    ): BarkPushResult {
        val response =
            runCatching { json.decodeFromString(BarkResponse.serializer(), body) }
                .getOrElse {
                    return BarkPushResult.Rejected(
                        httpStatus = httpStatus,
                        code = null,
                        message = "Invalid Bark response",
                    )
                }

        return if (httpStatus in 200..299 && response.code in 200..299) {
            BarkPushResult.Accepted(response.timestamp, response.code)
        } else {
            BarkPushResult.Rejected(
                httpStatus = httpStatus,
                code = response.code,
                message = response.message,
            )
        }
    }

    private fun logResponse(
        messageID: Long,
        httpStatus: Int,
        result: BarkPushResult,
    ) {
        when (result) {
            is BarkPushResult.Accepted -> {
                logger.info(
                    "Bark accepted message {}, httpStatus={}, code={}, timestamp={}",
                    messageID,
                    httpStatus,
                    result.code,
                    result.timestamp,
                )
            }

            is BarkPushResult.Rejected -> {
                logger.warn(
                    "Bark rejected message {}, httpStatus={}, code={}, reason={}",
                    messageID,
                    result.httpStatus,
                    result.code,
                    logText(result.message),
                )
            }

            BarkPushResult.Disabled -> Unit
        }
    }

    override fun close() {
        httpClient.close()
    }

    private companion object {
        const val DEFAULT_BODY = "收到一条新消息"
        const val ENCRYPTED_BODY = "Encrypted message"

        private fun createHttpClient(config: BarkConfig): HttpClient =
            HttpClient(CIO) {
                expectSuccess = false
                followRedirects = false
                install(HttpTimeout) {
                    val timeoutMillis = config.requestTimeout.toMillis()
                    requestTimeoutMillis = timeoutMillis
                    connectTimeoutMillis = timeoutMillis
                    socketTimeoutMillis = timeoutMillis
                }
            }
    }
}

data object DisabledBarkNotifier : BarkNotifier {
    override suspend fun notifyNewMessage(
        deviceKey: String,
        messageID: Long,
        message: BarkNotification,
    ): BarkPushResult = BarkPushResult.Disabled
}
