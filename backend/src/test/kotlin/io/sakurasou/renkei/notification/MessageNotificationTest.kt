package io.sakurasou.renkei.notification

import io.sakurasou.renkei.config.BarkConfig
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.message.RedeemedMessage
import io.sakurasou.renkei.model.dao.notification.NotificationTargetDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO
import io.sakurasou.renkei.model.entity.device.DevicePlatform
import io.sakurasou.renkei.model.entity.notification.NotificationProvider
import io.sakurasou.renkei.notification.bark.BarkNotifier
import io.sakurasou.renkei.notification.bark.BarkNotification
import io.sakurasou.renkei.notification.bark.BarkPushResult
import java.net.URI
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class MessageNotificationTest {
    @Test
    fun `dispatches Bark pushes only to subscribed iOS devices`() =
        runBlocking {
            val relations =
                FakeRelations(
                    listOf(
                        "iphone" to DevicePlatform.IOS,
                        "android" to DevicePlatform.ANDROID,
                    ),
                )
            val targets = FakeNotificationTargets(mutableMapOf("iphone" to "barkKey123"))
            val bark = FakeBarkNotifier(mutableListOf(BarkPushResult.Accepted(100)))
            val notification = MessageNotification(relations, targets, bark)

            notification.notify("publisher", 42, "10086", "message preview").join()

            assertEquals(
                listOf(BarkRequest("barkKey123", 42, BarkNotification("message preview", title = "10086"))),
                bark.requests,
            )
            notification.close()
        }

    @Test
    fun `retries a temporary Bark rejection`() =
        runBlocking {
            val relations = FakeRelations(listOf("iphone" to DevicePlatform.IOS))
            val targets = FakeNotificationTargets(mutableMapOf("iphone" to "barkKey123"))
            val bark =
                FakeBarkNotifier(
                    mutableListOf(
                        BarkPushResult.Rejected(503, 503, "unavailable"),
                        BarkPushResult.Accepted(100),
                    ),
                )
            val notification = MessageNotification(relations, targets, bark)

            notification.notify("publisher", 42, "10086", "message preview").join()

            assertEquals(2, bark.requests.size)
            notification.close()
        }

    @Test
    fun `skips a subscribed device without a Bark target`() =
        runBlocking {
            val relations = FakeRelations(listOf("iphone" to DevicePlatform.IOS))
            val targets = FakeNotificationTargets(mutableMapOf())
            val bark = FakeBarkNotifier(mutableListOf())
            val notification = MessageNotification(relations, targets, bark)

            notification.notify("publisher", 42, "10086", "message preview").join()

            assertEquals(emptyList(), bark.requests)
            notification.close()
        }

    @Test
    fun `uses a one-time link for a long Bark message`() =
        runBlocking {
            val relations = FakeRelations(listOf("iphone" to DevicePlatform.IOS))
            val targets = FakeNotificationTargets(mutableMapOf("iphone" to "barkKey123"))
            val bark = FakeBarkNotifier(mutableListOf(BarkPushResult.Accepted(100)))
            val tokens = FakeMessageAccessTokens("a".repeat(43))
            val config =
                BarkConfig(
                    baseURL = URI.create("https://api.day.app"),
                    publicBaseURL = URI.create("https://renkei.example"),
                    title = "RenKei",
                    group = "renkei",
                    encryptionKey = "12345678901234567890123456789012",
                    requestTimeout = Duration.ofSeconds(10),
                    messageLinkTTL = Duration.ofMinutes(10),
                )
            val notification = MessageNotification(relations, targets, bark, tokens, config)

            notification.notify("publisher", 42, "10086", "中".repeat(700)).join()

            assertEquals(42, tokens.messageID)
            assertEquals("iphone", tokens.subscriberDeviceID)
            assertEquals(1, bark.requests.size)
            assertEquals("10086", bark.requests.single().message.title)
            assertEquals(
                "https://renkei.example/notification-message#${"a".repeat(43)}",
                bark.requests.single().message.url,
            )
            notification.close()
        }

    private class FakeRelations(
        private val subscribers: List<Pair<String, DevicePlatform>>,
    ) : SubcribeRelationDAO {
        override suspend fun subscribe(
            providerDeviceID: String,
            subcriberDeviceID: String,
        ): Boolean = error("Not used")

        override suspend fun unsubscribe(
            providerDeviceID: String,
            subcriberDeviceID: String,
        ): Boolean = error("Not used")

        override suspend fun getSubcribers(providerDeviceID: String): List<Pair<String, DevicePlatform>> = subscribers

        override suspend fun isSubscribed(
            providerDeviceID: String,
            subcriberDeviceID: String,
        ): Boolean = subscribers.any { (deviceID, _) -> deviceID == subcriberDeviceID }
    }

    private class FakeNotificationTargets(
        private val values: MutableMap<String, String>,
    ) : NotificationTargetDAO {
        override suspend fun upsert(
            deviceUniqueID: String,
            provider: NotificationProvider,
            target: String,
        ) {
            values[deviceUniqueID] = target
        }

        override suspend fun getTarget(
            deviceUniqueID: String,
            provider: NotificationProvider,
        ): String? = values[deviceUniqueID]

        override suspend fun delete(
            deviceUniqueID: String,
            provider: NotificationProvider,
        ): Boolean = values.remove(deviceUniqueID) != null
    }

    private class FakeBarkNotifier(
        private val results: MutableList<BarkPushResult>,
    ) : BarkNotifier {
        val requests = mutableListOf<BarkRequest>()

        override suspend fun notifyNewMessage(
            deviceKey: String,
            messageID: Long,
            message: BarkNotification,
        ): BarkPushResult {
            requests += BarkRequest(deviceKey, messageID, message)
            return results.removeFirst()
        }
    }

    private class FakeMessageAccessTokens(
        private val token: String,
    ) : MessageAccessTokenDAO {
        var messageID: Long? = null
        var subscriberDeviceID: String? = null

        override suspend fun issue(
            messageID: Long,
            subscriberDeviceID: String,
            expiresAt: Long,
        ): String {
            this.messageID = messageID
            this.subscriberDeviceID = subscriberDeviceID
            return token
        }

        override suspend fun consume(
            token: String,
            now: Long,
        ): RedeemedMessage? = error("Not used")
    }

    private data class BarkRequest(
        val deviceKey: String,
        val messageID: Long,
        val message: BarkNotification,
    )
}
