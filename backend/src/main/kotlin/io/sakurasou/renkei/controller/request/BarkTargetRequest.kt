package io.sakurasou.renkei.controller.request

import kotlinx.serialization.Serializable

@Serializable
data class BarkTargetRequest(
    val deviceKey: String,
)
