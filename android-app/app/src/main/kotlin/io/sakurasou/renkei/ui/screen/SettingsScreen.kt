@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package io.sakurasou.renkei.ui.screen

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sakurasou.renkei.ui.viewmodel.SettingsUiState
import kotlin.text.ifEmpty

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRegenerateKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showRegistrationDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnAction by rememberUpdatedState(onAction)
    val context = LocalContext.current
    val machineID =
        remember(context) {
            Settings.Secure
                .getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                .orEmpty()
        }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(machineID, uiState.identifier) {
        if (machineID.isNotBlank() && machineID != uiState.identifier) {
            currentOnAction(SettingsAction.ChangeIdentifier(machineID))
        }
    }

    SettingsContent(
        uiState = uiState,
        machineID = machineID,
        onAction = onAction,
        onRequestRegistration = { showRegistrationDialog = true },
        onRequestRegenerate = { showRegenerateKeyDialog = true },
        modifier = modifier,
    )

    if (showRegenerateKeyDialog) {
        RegenerateKeyDialog(
            onConfirm = {
                showRegenerateKeyDialog = false
                onAction(SettingsAction.RegenerateKeyPair)
            },
            onDismiss = { showRegenerateKeyDialog = false },
        )
    }

    if (showRegistrationDialog) {
        RegistrationDialog(
            onConfirm = { username, password ->
                showRegistrationDialog = false
                onAction(SettingsAction.Register(username, password))
            },
            onDismiss = { showRegistrationDialog = false },
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    machineID: String,
    onAction: (SettingsAction) -> Unit,
    onRequestRegistration: () -> Unit,
    onRequestRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val cardModifier = Modifier.fillMaxWidth()
        MachineIDCard(machineID, cardModifier)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        ServerConfigCard(uiState, onAction, onRequestRegistration, cardModifier)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        EncryptConfigCard(uiState, onRequestRegenerate, cardModifier)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        CryptoTestCard(uiState, onAction, cardModifier)
    }
}

@Composable
private fun RegistrationDialog(
    onConfirm: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注册设备") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "请输入服务器的 Basic 鉴权信息。鉴权信息仅用于本次注册，不会保存。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank(),
            ) {
                Text("注册")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun RegenerateKeyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重新生成密钥对？") },
        text = { Text("旧私钥将无法恢复，也将无法解密由旧公钥加密的数据。若设备已注册，生成后会自动同步新公钥。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("重新生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun MachineIDCard(
    machineID: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "设备标识",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "由 Android 系统提供，用于在服务器中识别当前设备。",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = machineID.ifBlank { "不可用" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ANDROID_ID") },
                readOnly = true,
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ServerConfigCard(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onRequestRegistration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "服务器配置",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = uiState.serverAddress,
                onValueChange = { onAction(SettingsAction.ChangeServerAddress(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地址") },
                placeholder = { Text("https://example.com") },
                supportingText = uiState.serverAddressError?.let { error -> { Text(error) } },
                isError = uiState.serverAddressError != null,
                enabled = !uiState.isServerOperationInProgress,
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.serverPort,
                onValueChange = { onAction(SettingsAction.ChangeServerPort(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("端口") },
                supportingText = uiState.serverPortError?.let { error -> { Text(error) } },
                isError = uiState.serverPortError != null,
                enabled = !uiState.isServerOperationInProgress,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "状态：${uiState.registrationStatusText()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (uiState.hasRegistration && !uiState.isDirty) {
                    OutlinedButton(
                        onClick = { onAction(SettingsAction.TestServer) },
                        enabled = !uiState.isServerOperationInProgress,
                    ) {
                        Text(if (uiState.isTestingServer) "测试中…" else "测试")
                    }
                }
                Button(
                    onClick = onRequestRegistration,
                    enabled = !uiState.isServerOperationInProgress,
                ) {
                    Text(if (uiState.isRegistering) "注册中…" else "注册")
                }
            }
        }
    }
}

private val SettingsUiState.isServerOperationInProgress: Boolean
    get() = isRegistering || isTestingServer || isCryptoBusy

private fun SettingsUiState.registrationStatusText(): String =
    when {
        isRegistering -> "注册中…"
        isTestingServer -> "测试中…"
        isCryptoBusy && hasRegistration -> "正在同步公钥…"
        hasRegistration && !isPublicKeySynchronized -> "公钥同步失败"
        hasRegistration && isDirty -> "配置已修改，待重新注册"
        hasRegistration -> "已注册"
        else -> "未注册"
    }

@Composable
private fun EncryptConfigCard(
    uiState: SettingsUiState,
    onRequestRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "加密配置",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                if (uiState.hasKeyPair) {
                    "私钥已安全保存在 Android Keystore 中，不可导出。"
                } else {
                    "尚未生成密钥对。"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = uiState.publicKey,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("公钥（Base64）") },
                placeholder = { Text("生成密钥对后显示") },
                readOnly = true,
                minLines = 3,
                maxLines = 5,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRequestRegenerate,
                    enabled =
                        !uiState.isCryptoBusy &&
                            !uiState.isRegistering &&
                            !uiState.isTestingServer,
                ) {
                    Text(if (uiState.isCryptoBusy) "处理中…" else "重新生成密钥")
                }
            }
        }
    }
}

@Composable
private fun CryptoTestCard(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "加密/解密测试",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "加密后可直接点击解密验证；也可以粘贴 Base64 密文后解密。",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = uiState.testContent,
                onValueChange = { onAction(SettingsAction.ChangeTestContent(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("内容") },
                enabled = !uiState.isCryptoBusy,
                minLines = 3,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onAction(SettingsAction.EncryptTestContent) },
                    enabled = uiState.hasKeyPair && !uiState.isCryptoBusy,
                ) {
                    Text("加密")
                }
                Button(
                    onClick = { onAction(SettingsAction.DecryptTestContent) },
                    enabled = uiState.hasKeyPair && !uiState.isCryptoBusy,
                ) {
                    Text("解密")
                }
            }
            OutlinedTextField(
                value = uiState.testResult.ifEmpty { "暂无结果" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("结果") },
                placeholder = { Text("生成密钥对后显示") },
                readOnly = true,
                minLines = 3,
                maxLines = 5,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateFunction")
private fun SettingsScreenPreview() {
    SettingsScreen(
        uiState =
        SettingsUiState(
            isLoading = false,
            serverAddress = "https://example.com",
            serverPort = "443",
            identifier = "my-phone",
            hasKeyPair = true,
            publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...",
        ),
        onAction = {},
    )
}
