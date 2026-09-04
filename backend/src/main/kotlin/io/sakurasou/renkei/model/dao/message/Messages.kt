package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.model.dao.device.Devices
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * @author Shiina Kin
 * 2026/9/1 17:20
 */
object Messages : LongIdTable() {
    val deviceID = reference("device_id", Devices)
    val title = varchar("title", 255).default("")
    val previewContent = varchar("preview_content", 255)
    val content = text("content")
    val timestamp = long("timestamp")
}
