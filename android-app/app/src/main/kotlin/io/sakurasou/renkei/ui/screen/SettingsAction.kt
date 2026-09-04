package io.sakurasou.renkei.ui.screen

sealed interface SettingsAction {
    data class ChangeServerAddress(val value: String) : SettingsAction

    data class ChangeServerPort(val value: String) : SettingsAction

    data class ChangeIdentifier(val value: String) : SettingsAction

    data class ChangeTestContent(val value: String) : SettingsAction

    data class Register(
        val username: String,
        val password: String,
    ) : SettingsAction

    data object TestServer : SettingsAction

    data object RegenerateKeyPair : SettingsAction

    data object EncryptTestContent : SettingsAction

    data object DecryptTestContent : SettingsAction

    data object DismissMessage : SettingsAction
}
