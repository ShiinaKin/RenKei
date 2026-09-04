package io.sakurasou.renkei.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sakurasou.renkei.crypto.CryptoKeyInfo
import io.sakurasou.renkei.crypto.CryptoKeyRepository
import io.sakurasou.renkei.data.SettingsRepository
import io.sakurasou.renkei.network.RenkeiClient
import io.sakurasou.renkei.network.ServerRequestException
import io.sakurasou.renkei.settings.AppSettings
import io.sakurasou.renkei.ui.screen.SettingsAction
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_PORT = 1
private const val MAX_PORT = 65_535
private const val MAX_PORT_LENGTH = 5
private const val MAX_IDENTIFIER_LENGTH = 128
private const val HTTP_UNAUTHORIZED = 401
private const val SERVER_TEST_MESSAGE = "RenKei Android 连接测试"
private val supportedSchemes = setOf("http", "https")

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isRegistering: Boolean = false,
    val isTestingServer: Boolean = false,
    val isCryptoBusy: Boolean = false,
    val isDirty: Boolean = false,
    val hasRegistration: Boolean = false,
    val isPublicKeySynchronized: Boolean = true,
    val serverAddress: String = "",
    val serverPort: String = AppSettings.DEFAULT_SERVER_PORT.toString(),
    val identifier: String = "",
    val serverAddressError: String? = null,
    val serverPortError: String? = null,
    val identifierError: String? = null,
    val hasKeyPair: Boolean = false,
    val publicKey: String = "",
    val testContent: String = "",
    val testResult: String = "",
    val message: String? = null,
    val isMessageError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val cryptoKeyRepository: CryptoKeyRepository,
    private val renkeiClient: RenkeiClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var registrationToken: String = ""
    private var registeredSettings: AppSettings? = null

    init {
        loadSettings()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ChangeServerAddress ->
                _uiState.update { state ->
                    state.copy(
                        serverAddress = action.value,
                        serverAddressError = null,
                        isDirty = true,
                        message = null,
                        isMessageError = false,
                    )
                }
            is SettingsAction.ChangeServerPort ->
                if (
                    action.value.length <= MAX_PORT_LENGTH &&
                    action.value.all(Char::isDigit)
                ) {
                    _uiState.update { state ->
                        state.copy(
                            serverPort = action.value,
                            serverPortError = null,
                            isDirty = true,
                            message = null,
                            isMessageError = false,
                        )
                    }
                }
            is SettingsAction.ChangeIdentifier ->
                _uiState.update { state ->
                    state.copy(
                        identifier = action.value,
                        identifierError = null,
                        isDirty = true,
                        message = null,
                        isMessageError = false,
                    )
                }
            is SettingsAction.ChangeTestContent ->
                _uiState.update { state ->
                    state.copy(
                        testContent = action.value,
                        testResult = "",
                        message = null,
                        isMessageError = false,
                    )
                }
            is SettingsAction.Register -> register(action.username, action.password)
            SettingsAction.TestServer -> testServer()
            SettingsAction.RegenerateKeyPair -> regenerateKeyPair()
            SettingsAction.EncryptTestContent -> encryptTestContent()
            SettingsAction.DecryptTestContent -> decryptTestContent()
            SettingsAction.DismissMessage -> dismissMessage()
        }
    }

    private fun dismissMessage() {
        _uiState.update { state ->
            state.copy(message = null, isMessageError = false)
        }
    }

    private fun register(
        username: String,
        password: String,
    ) {
        val validation = validateSettings(_uiState.value)
        val settings = validation.settings
        if (settings == null) {
            _uiState.update { state ->
                state.copy(
                    serverAddressError = validation.serverAddressError,
                    serverPortError = validation.serverPortError,
                    identifierError = validation.identifierError,
                    message = "请修正配置后再注册",
                    isMessageError = true,
                )
            }
            return
        }
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { state ->
                state.copy(message = "Basic 鉴权的用户名和密码不能为空", isMessageError = true)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isRegistering = true, message = null, isMessageError = false)
            }
            runCatching { performRegistration(settings, username, password) }
                .onSuccess { result ->
                    registrationToken = result.token
                    registeredSettings = result.settings
                    _uiState.update { state ->
                        state.copy(
                            isRegistering = false,
                            isDirty = false,
                            hasRegistration = true,
                            isPublicKeySynchronized = true,
                            hasKeyPair = result.keyInfo.hasKeyPair,
                            publicKey = result.keyInfo.publicKey,
                            message = "设备注册成功",
                            isMessageError = false,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isRegistering = false,
                            message = error.registrationErrorMessage(),
                            isMessageError = true,
                        )
                    }
                }
        }
    }

    private suspend fun performRegistration(
        settings: AppSettings,
        username: String,
        password: String,
    ): RegistrationResult {
        val keyInfo = cryptoKeyRepository.getKeyInfo()
        check(keyInfo.hasKeyPair && keyInfo.publicKey.isNotBlank()) {
            "未检测到有效密钥，请等待自动生成完成后重试"
        }
        val token =
            renkeiClient.registerDevice(
                settings = settings,
                deviceName = currentDeviceName(),
                publicKey = keyInfo.publicKey,
                username = username,
                password = password,
            )
        val persistedSettings = settings.copy(token = token)
        settingsRepository.update(persistedSettings)
        return RegistrationResult(token, persistedSettings, keyInfo)
    }

    private fun testServer() {
        val state = _uiState.value
        if (!state.hasRegistration || registrationToken.isBlank() || state.isDirty) return
        val validation = validateSettings(state)
        val settings = validation.settings ?: return

        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isTestingServer = true, message = null, isMessageError = false)
            }
            runCatching {
                val cipherText = cryptoKeyRepository.encryptWithPrivateKey(SERVER_TEST_MESSAGE)
                val response = renkeiClient.sendTestMessage(settings, registrationToken, cipherText)
                check(response == SERVER_TEST_MESSAGE) { "测试失败：服务端返回内容不一致" }
            }
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isTestingServer = false,
                            message = "测试消息发送成功",
                            isMessageError = false,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isTestingServer = false,
                            message = error.testErrorMessage(),
                            isMessageError = true,
                        )
                    }
                }
        }
    }

    private fun regenerateKeyPair() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isCryptoBusy = true, message = null, isMessageError = false)
            }
            val keyInfo =
                runCatching { cryptoKeyRepository.regenerateKeyPair() }
                    .getOrElse { error ->
                        _uiState.update { state ->
                            state.copy(
                                isCryptoBusy = false,
                                message = error.userMessage("重新生成密钥失败"),
                                isMessageError = true,
                            )
                        }
                        return@launch
                    }
            synchronizeRegeneratedKey(keyInfo)
        }
    }

    private suspend fun synchronizeRegeneratedKey(keyInfo: CryptoKeyInfo) {
        val settings = registeredSettings
        val shouldSynchronize = settings != null && registrationToken.isNotBlank()
        _uiState.update { state ->
            state.copy(
                hasKeyPair = true,
                publicKey = keyInfo.publicKey,
                testResult = "",
                isPublicKeySynchronized = !shouldSynchronize,
            )
        }
        if (!shouldSynchronize) {
            _uiState.update { state ->
                state.copy(
                    isCryptoBusy = false,
                    message = "密钥已重新生成，注册设备时将同步公钥",
                    isMessageError = false,
                )
            }
            return
        }
        runCatching {
            renkeiClient.updatePublicKey(
                settings = checkNotNull(settings),
                token = registrationToken,
                publicKey = keyInfo.publicKey,
            )
        }.onSuccess {
            _uiState.finishPublicKeySynchronization()
        }.onFailure { error ->
            _uiState.failPublicKeySynchronization(error)
        }
    }

    private fun encryptTestContent() {
        val content = _uiState.value.testContent
        if (content.isBlank()) {
            _uiState.update { state ->
                state.copy(message = "请输入要加密的内容", isMessageError = true)
            }
            return
        }
        runCryptoTextOperation(successMessage = "加密完成") {
            cryptoKeyRepository.encrypt(content)
        }
    }

    private fun decryptTestContent() {
        val state = _uiState.value
        val cipherText = state.testResult.ifBlank { state.testContent }
        if (cipherText.isBlank()) {
            _uiState.update { currentState ->
                currentState.copy(message = "请输入 Base64 密文", isMessageError = true)
            }
            return
        }
        runCryptoTextOperation(successMessage = "解密完成") {
            cryptoKeyRepository.decrypt(cipherText)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settingsResult = runCatching { settingsRepository.settings.first() }
            val keyInfoResult = runCatching { cryptoKeyRepository.generateKeyPair() }
            val settings = settingsResult.getOrDefault(AppSettings())
            registrationToken = settings.token
            registeredSettings = settings.takeIf { it.token.isNotBlank() }
            val keyInfo = keyInfoResult.getOrDefault(CryptoKeyInfo(false, ""))
            val error = settingsResult.exceptionOrNull() ?: keyInfoResult.exceptionOrNull()

            _uiState.value =
                SettingsUiState(
                    isLoading = false,
                    serverAddress = settings.serverAddress,
                    serverPort = settings.serverPort.toString(),
                    identifier = settings.identifier,
                    hasRegistration = settings.token.isNotBlank(),
                    hasKeyPair = keyInfo.hasKeyPair,
                    publicKey = keyInfo.publicKey,
                    message = error?.userMessage("加载设置失败"),
                    isMessageError = error != null,
                )
        }
    }

    private fun runCryptoTextOperation(
        successMessage: String,
        operation: suspend () -> String,
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isCryptoBusy = true, message = null, isMessageError = false)
            }
            runCatching { operation() }
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            isCryptoBusy = false,
                            testResult = result,
                            message = successMessage,
                            isMessageError = false,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isCryptoBusy = false,
                            message = error.userMessage("加解密失败"),
                            isMessageError = true,
                        )
                    }
                }
        }
    }
}

private fun MutableStateFlow<SettingsUiState>.finishPublicKeySynchronization() {
    update { state ->
        state.copy(
            isCryptoBusy = false,
            isPublicKeySynchronized = true,
            message = "密钥已重新生成并同步到服务器",
            isMessageError = false,
        )
    }
}

private fun MutableStateFlow<SettingsUiState>.failPublicKeySynchronization(error: Throwable) {
    update { state ->
        state.copy(
            isCryptoBusy = false,
            isPublicKeySynchronized = false,
            message = error.publicKeyUpdateErrorMessage(),
            isMessageError = true,
        )
    }
}

private fun validateSettings(state: SettingsUiState): SettingsValidation {
    val address = validateAddress(state.serverAddress)
    val port = validatePort(state.serverPort)
    val identifier = validateIdentifier(state.identifier)
    val hasErrors = address.error != null || port.error != null || identifier.error != null

    return SettingsValidation(
        settings =
            if (hasErrors) {
                null
            } else {
                AppSettings(
                    serverAddress = address.value,
                    serverPort = checkNotNull(port.value),
                    identifier = identifier.value,
                )
            },
        serverAddressError = address.error,
        serverPortError = port.error,
        identifierError = identifier.error,
    )
}

private fun validateAddress(value: String): ValidatedField<String> {
    val address = value.trim().trimEnd('/')
    val uri = runCatching { URI(address) }.getOrNull()
    val isValid =
        uri != null &&
            uri.scheme in supportedSchemes &&
            !uri.host.isNullOrBlank() &&
            uri.port == -1 &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null &&
            (uri.path.isNullOrEmpty() || uri.path == "/")
    return ValidatedField(
        value = address,
        error = if (isValid) null else "请输入不含端口和路径的 http/https 地址",
    )
}

private fun validatePort(value: String): ValidatedField<Int?> {
    val port = value.toIntOrNull()
    return ValidatedField(
        value = port,
        error = if (port != null && port in MIN_PORT..MAX_PORT) null else "端口必须在 1–65535 之间",
    )
}

private fun validateIdentifier(value: String): ValidatedField<String> {
    val identifier = value.trim()
    val error =
        when {
            identifier.isEmpty() -> "标识符不能为空"
            identifier.length > MAX_IDENTIFIER_LENGTH -> "标识符不能超过 128 个字符"
            else -> null
        }
    return ValidatedField(value = identifier, error = error)
}

private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf(String::isNotBlank) ?: fallback

private fun Throwable.registrationErrorMessage(): String =
    when {
        this is ServerRequestException && statusCode == HTTP_UNAUTHORIZED -> "Basic 鉴权失败，请检查用户名和密码"
        this is ServerRequestException -> "注册失败：服务器返回 HTTP $statusCode"
        else -> userMessage("设备注册失败")
    }

private fun Throwable.testErrorMessage(): String =
    when {
        this is ServerRequestException && statusCode == HTTP_UNAUTHORIZED ->
            "测试失败：token 无效或已过期，请重新注册"
        this is ServerRequestException -> "测试失败：服务器返回 HTTP $statusCode"
        else -> userMessage("测试消息发送失败")
    }

private fun Throwable.publicKeyUpdateErrorMessage(): String =
    when {
        this is ServerRequestException && statusCode == HTTP_UNAUTHORIZED ->
            "密钥已重新生成，但 token 无效或已过期，公钥同步失败"
        this is ServerRequestException ->
            "密钥已重新生成，但服务器返回 HTTP $statusCode，公钥同步失败"
        else -> userMessage("密钥已重新生成，但公钥同步失败")
    }

private fun currentDeviceName(): String =
    listOf(Build.MANUFACTURER, Build.MODEL)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" ")
        .ifBlank { "Android device" }

private data class ValidatedField<T>(
    val value: T,
    val error: String?,
)

private data class SettingsValidation(
    val settings: AppSettings?,
    val serverAddressError: String?,
    val serverPortError: String?,
    val identifierError: String?,
)

private data class RegistrationResult(
    val token: String,
    val settings: AppSettings,
    val keyInfo: CryptoKeyInfo,
)
