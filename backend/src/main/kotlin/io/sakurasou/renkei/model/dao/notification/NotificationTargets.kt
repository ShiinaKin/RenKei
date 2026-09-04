package io.sakurasou.renkei.model.dao.notification

import io.sakurasou.renkei.model.dao.device.Devices
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object NotificationTargets : Table("notification_targets") {
    val deviceUniqueID =
        varchar("device_unique_id", 255)
            .references(Devices.uniqueID, onDelete = ReferenceOption.CASCADE)
    val provider = varchar("provider", 32)
    val target = varchar("target", 512)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(deviceUniqueID, provider)
}
