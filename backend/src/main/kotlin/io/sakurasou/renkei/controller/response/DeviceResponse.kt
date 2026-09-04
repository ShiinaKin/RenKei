package io.sakurasou.renkei.controller.response

import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegistrationResponse(
    val token: String,
)
