package io.sakurasou.renkei.model.dao.device

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * @author Shiina Kin
 * 2026/9/1 17:20
 */
object Devices : LongIdTable() {
    val name = varchar("name", 255)
    val uniqueID = varchar("unique_id", 255).uniqueIndex()
    val platform = varchar("platform", 255)
    val publicKey = text("public_key")
}
