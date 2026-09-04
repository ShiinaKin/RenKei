package io.sakurasou.renkei.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val serverAddress: String = "",
    val serverPort: Int = DEFAULT_SERVER_PORT,
    val identifier: String = "",
    val token: String = "",
) {
    companion object {
        const val DEFAULT_SERVER_PORT = 443
    }
}
