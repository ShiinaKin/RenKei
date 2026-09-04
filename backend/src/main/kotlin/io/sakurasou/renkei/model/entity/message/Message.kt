package io.sakurasou.renkei.model.entity.message

import kotlinx.serialization.Serializable

/**
 * @author Shiina Kin
 * 2026/9/1 17:17
 */
@Serializable
data class Message(
    val id: Long? = null,
    val deviceUniqueID: String,
    val title: String,
    val previewContent: String,
    val content: String,
    val timestamp: Long,
)
