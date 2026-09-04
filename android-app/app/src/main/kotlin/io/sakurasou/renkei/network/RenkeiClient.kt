package io.sakurasou.renkei.network

import io.sakurasou.renkei.settings.AppSettings
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
private val textMediaType = "text/plain; charset=utf-8".toMediaType()

@Singleton
class RenkeiClient
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun registerDevice(
        settings: AppSettings,
        deviceName: String,
        publicKey: String,
        username: String,
        password: String,
    ): String =
        withContext(Dispatchers.IO) {
            val payload =
                DeviceRegistrationRequest(
                    deviceName = deviceName,
                    uniqueID = settings.identifier,
                    platform = DevicePlatform.ANDROID,
                    publicKey = publicKey,
                )
            val request =
                Request
                    .Builder()
                    .url(settings.endpoint("device/regist"))
                    .header(
                        "Authorization",
                        Credentials.basic(username, password, StandardCharsets.UTF_8),
                    ).post(JSON.encodeToString(payload).toRequestBody(jsonMediaType))
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw ServerRequestException(response.code, body)
                }
                JSON.decodeFromString<DeviceRegistrationResponse>(body)
                    .token
                    .takeIf(String::isNotBlank)
                    ?: error("服务器返回了空 token")
            }
        }

    suspend fun updatePublicKey(
        settings: AppSettings,
        token: String,
        publicKey: String,
    ) =
        withContext(Dispatchers.IO) {
            val payload = PublicKeyUpdateRequest(publicKey)
            val request =
                Request
                    .Builder()
                    .url(settings.endpoint("device/public-key"))
                    .header("Authorization", "Bearer $token")
                    .patch(JSON.encodeToString(payload).toRequestBody(jsonMediaType))
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw ServerRequestException(response.code, body)
                }
            }
        }

    suspend fun sendTestMessage(
        settings: AppSettings,
        token: String,
        cipherText: String,
    ): String =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(settings.endpoint("message/send-test"))
                    .header("Authorization", "Bearer $token")
                    .post(cipherText.toRequestBody(textMediaType))
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw ServerRequestException(response.code, body)
                }
                body
            }
        }

    suspend fun sendMessage(
        settings: AppSettings,
        token: String,
        cipherText: String,
    ) =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(settings.endpoint("message/send"))
                    .header("Authorization", "Bearer $token")
                    .post(cipherText.toRequestBody(textMediaType))
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw ServerRequestException(response.code, body)
                }
            }
        }
}

class ServerRequestException(
    val statusCode: Int,
    responseBody: String,
) : RuntimeException(
        responseBody.takeIf(String::isNotBlank)?.let { "HTTP $statusCode: $it" }
            ?: "HTTP $statusCode",
    )

@Serializable
private data class DeviceRegistrationRequest(
    val deviceName: String,
    val uniqueID: String,
    val platform: DevicePlatform,
    val publicKey: String,
)

@Serializable
private data class PublicKeyUpdateRequest(
    val publicKey: String,
)

@Serializable
private enum class DevicePlatform {
    ANDROID,
}

@Serializable
private data class DeviceRegistrationResponse(
    val token: String,
)

private fun AppSettings.endpoint(path: String): String =
    "${serverAddress.trimEnd('/')}:$serverPort/${path.trimStart('/')}"
