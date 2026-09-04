package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.model.dao.device.Devices
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object MessageAccessTokens : Table("message_access_tokens") {
    val tokenHash = varchar("token_hash", 64)
    val messageID = reference("message_id", Messages, onDelete = ReferenceOption.CASCADE)
    val subscriberDeviceID =
        varchar("subscriber_device_id", 255)
            .references(Devices.uniqueID, onDelete = ReferenceOption.CASCADE)
    val expiresAt = long("expires_at")
    val consumedAt = long("consumed_at").nullable()

    override val primaryKey = PrimaryKey(tokenHash)
}
