package io.sakurasou.renkei.model.dao.notification

import io.sakurasou.renkei.db.DatabaseSingleton
import io.sakurasou.renkei.model.entity.notification.NotificationProvider
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface NotificationTargetDAO {
    suspend fun upsert(
        deviceUniqueID: String,
        provider: NotificationProvider,
        target: String,
    )

    suspend fun getTarget(
        deviceUniqueID: String,
        provider: NotificationProvider,
    ): String?

    suspend fun delete(
        deviceUniqueID: String,
        provider: NotificationProvider,
    ): Boolean
}

class SqlNotificationTargetDAO : NotificationTargetDAO {
    override suspend fun upsert(
        deviceUniqueID: String,
        provider: NotificationProvider,
        target: String,
    ) {
        DatabaseSingleton.dbQuery {
            val now = System.currentTimeMillis()
            val providerName = provider.name
            val updated =
                NotificationTargets.update({
                    (NotificationTargets.deviceUniqueID eq deviceUniqueID) and
                        (NotificationTargets.provider eq providerName)
                }) {
                    it[NotificationTargets.target] = target
                    it[updatedAt] = now
                }
            if (updated == 0) {
                NotificationTargets.insert {
                    it[NotificationTargets.deviceUniqueID] = deviceUniqueID
                    it[NotificationTargets.provider] = providerName
                    it[NotificationTargets.target] = target
                    it[updatedAt] = now
                }
            }
        }
    }

    override suspend fun getTarget(
        deviceUniqueID: String,
        provider: NotificationProvider,
    ): String? =
        DatabaseSingleton.dbQuery {
            NotificationTargets
                .selectAll()
                .where {
                    (NotificationTargets.deviceUniqueID eq deviceUniqueID) and
                        (NotificationTargets.provider eq provider.name)
                }.singleOrNull()
                ?.get(NotificationTargets.target)
        }

    override suspend fun delete(
        deviceUniqueID: String,
        provider: NotificationProvider,
    ): Boolean =
        DatabaseSingleton.dbQuery {
            NotificationTargets.deleteWhere {
                (NotificationTargets.deviceUniqueID eq deviceUniqueID) and
                    (NotificationTargets.provider eq provider.name)
            } > 0
        }
}
