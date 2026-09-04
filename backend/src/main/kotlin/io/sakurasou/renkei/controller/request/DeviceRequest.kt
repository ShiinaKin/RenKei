package io.sakurasou.renkei.controller.request

import io.sakurasou.renkei.model.entity.device.DevicePlatform
import kotlinx.serialization.Serializable

/**
 * @author Shiina Kin
 * 2026/9/2 00:19
 */

@Serializable
data class DeviceRegistRequest(
    val deviceName: String,
    val uniqueID: String,
    val platform: DevicePlatform,
    val publicKey: String,
)

@Serializable
data class DevicePublicKeyUpdateRequest(
    val publicKey: String,
)
