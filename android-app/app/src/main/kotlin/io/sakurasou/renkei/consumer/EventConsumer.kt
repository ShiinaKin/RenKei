package io.sakurasou.renkei.consumer

import io.sakurasou.renkei.crypto.CryptoKeyRepository
import io.sakurasou.renkei.data.SettingsRepository
import io.sakurasou.renkei.module.entity.IncomingCallEvent
import io.sakurasou.renkei.module.entity.SMSEvent
import io.sakurasou.renkei.network.RenkeiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @author Shiina Kin
 * 2026/8/19 00:03
 */
@Singleton
class EventConsumer
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val cryptoKeyRepository: CryptoKeyRepository,
    private val renkeiClient: RenkeiClient,
) {
    suspend fun onSMSEvent(event: SMSEvent) {
        send(
            title = event.number,
            content = event.message,
        )
    }

    suspend fun onIncomingCallEvent(event: IncomingCallEvent) {
        send(
            title = event.number,
            content = "来电",
        )
    }

    suspend fun sendSimulatedNotification() {
        send(
            title = "RenKei 模拟通知",
            content = "这是一条从 Android 端发出的端到端测试消息。",
        )
    }

    private suspend fun send(
        title: String,
        content: String,
    ) {
        val settings = settingsRepository.settings.first()
        require(settings.token.isNotBlank()) { "设备尚未注册" }
        val payload = Json.encodeToString(ReportedMessage(title, content))
        val cipherText = cryptoKeyRepository.encryptWithPrivateKey(payload)
        renkeiClient.sendMessage(settings, settings.token, cipherText)
    }
}

@Serializable
internal data class ReportedMessage(
    val title: String,
    val content: String,
)
