package io.sakurasou.renkei.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.sakurasou.renkei.config.exceptionHandler
import io.sakurasou.renkei.controller.authenticatedDeviceRoutes
import io.sakurasou.renkei.controller.deviceRoutes
import io.sakurasou.renkei.controller.messageRoute
import io.sakurasou.renkei.controller.notificationMessageRoutes
import io.sakurasou.renkei.controller.notificationTargetRoutes
import io.sakurasou.renkei.controller.subscriptionRoutes
import io.sakurasou.renkei.di.inject
import io.sakurasou.renkei.model.dao.device.DeviceDAO
import io.sakurasou.renkei.model.dao.notification.NotificationTargetDAO
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import io.sakurasou.renkei.model.dao.relation.SubcribeRelationDAO

fun Application.configureRouting() {
    val notificationTargetDAO by inject<NotificationTargetDAO>()
    val deviceDAO by inject<DeviceDAO>()
    val subscribeRelationDAO by inject<SubcribeRelationDAO>()
    val messageAccessTokenDAO by inject<MessageAccessTokenDAO>()
    install(StatusPages) {
        exceptionHandler()
    }
    routing {
        notificationMessageRoutes(messageAccessTokenDAO)
        authenticate("auth-basic") {
            deviceRoutes()
        }
        authenticate("auth-jwt") {
            authenticatedDeviceRoutes()
            messageRoute()
            notificationTargetRoutes(notificationTargetDAO, deviceDAO)
            subscriptionRoutes(subscribeRelationDAO, deviceDAO)
        }
    }
}
