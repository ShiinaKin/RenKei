package io.sakurasou.renkei.di

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.sakurasou.renkei.config.loadBarkConfig
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.dao.device.DeviceDAOImpl
import io.sakurasou.renkei.model.dao.message.MessageDAO
import io.sakurasou.renkei.model.dao.message.MessageDAOImpl
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.message.SqlMessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.notification.NotificationTargetDAO
import io.sakurasou.renkei.model.dao.notification.SqlNotificationTargetDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAOImpl
import io.sakurasou.renkei.notification.MessageNotification
import io.sakurasou.renkei.notification.bark.BarkClient
import io.sakurasou.renkei.notification.bark.BarkNotifier
import io.sakurasou.renkei.notification.bark.DisabledBarkNotifier
import org.slf4j.LoggerFactory

object InstanceCenter {
    private var notification: MessageNotification? = null
    private var barkNotifier: BarkNotifier? = null

    fun init(application: Application) {
        val barkConfig = application.loadBarkConfig()
        diOperation {
            regist<DeviceDAO> { DeviceDAOImpl() }
            regist<MessageDAO> { MessageDAOImpl() }
            regist<MessageAccessTokenDAO> { SqlMessageAccessTokenDAO() }
            regist<SubcribeRelationDAO> { SubcribeRelationDAOImpl() }
            regist<NotificationTargetDAO> { SqlNotificationTargetDAO() }
            regist<BarkNotifier> {
                barkConfig?.let(::BarkClient) ?: DisabledBarkNotifier
            }
            regist { scope ->
                MessageNotification(
                    subscribeRelationDAO = scope.get(),
                    notificationTargetDAO = scope.get(),
                    barkNotifier = scope.get(),
                    messageAccessTokenDAO = scope.get(),
                    barkConfig = barkConfig,
                )
            }
        }
        notification = DIManager.getDIInstance().get()
        barkNotifier = DIManager.getDIInstance().get()
        logger.info(
            "Application dependencies initialized: barkEnabled={}, barkHost={}",
            barkConfig != null,
            barkConfig?.baseURL?.host ?: "disabled",
        )
    }

    fun close() {
        notification?.close()
        (barkNotifier as? AutoCloseable)?.close()
        notification = null
        barkNotifier = null
        logger.info("Application dependencies closed")
    }

    private val logger = LoggerFactory.getLogger(InstanceCenter::class.java)
}

fun Application.configureDependencies() {
    InstanceCenter.init(this)
    monitor.subscribe(ApplicationStopped) {
        InstanceCenter.close()
    }
}
