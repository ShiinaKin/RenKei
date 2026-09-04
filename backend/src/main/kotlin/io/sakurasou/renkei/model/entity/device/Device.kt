package io.sakurasou.renkei.model.entity.device

import kotlinx.serialization.Serializable

/**
 * @author Shiina Kin
 * 2026/9/1 17:15
 */
data class Device(
    val id: Long? = null,
    val name: String,
    val uniqueID: String,
    val platform: DevicePlatform,
    val publicKey: String,
)

@Serializable
enum class DevicePlatform(
    val platform: String,
) {
    ANDROID("android"),
    IOS("ios"),
    WINDOWS("windows"),
    MACOS("macos"),
    LINUX("linux"),
}
