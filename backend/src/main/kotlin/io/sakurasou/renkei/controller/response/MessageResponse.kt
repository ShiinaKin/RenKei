package io.sakurasou.renkei.controller.response

/**
 * @author Shiina Kin
 * 2026/9/2 01:04
 */

data class MessageDetailResponse(
    val id: Long,
    val title: String,
    val previewContent: String,
    val content: String,
    val timestamp: Long,
)
