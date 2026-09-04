package io.sakurasou.renkei.controller.request

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val title: String,
    val content: String,
)
