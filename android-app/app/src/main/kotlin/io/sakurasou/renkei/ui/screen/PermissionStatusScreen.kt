@file:Suppress("FunctionNaming")

package io.sakurasou.renkei.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sakurasou.renkei.permission.AppPermission
import io.sakurasou.renkei.permission.PermissionStatus
import io.sakurasou.renkei.sms.SmsReceipt
import io.sakurasou.renkei.ui.component.PermissionCard
import io.sakurasou.renkei.ui.theme.RenKeiTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun PermissionStatusScreen(
    permissions: List<PermissionStatus>,
    callScreeningRoleGranted: Boolean,
    callScreeningRoleAvailable: Boolean,
    latestReceipt: SmsReceipt?,
    onRequestPermission: (AppPermission) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMissingPermission = permissions.any { !it.granted }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("RenKei 权限中心", style = MaterialTheme.typography.headlineLarge)
            Text(
                "这里集中准备短信转发和来电通知所需的 Android 权限。所有申请都由你主动触发。",
                style = MaterialTheme.typography.bodyLarge,
            )

            PermissionRequirements(
                permissions = permissions,
                callScreeningRoleGranted = callScreeningRoleGranted,
                callScreeningRoleAvailable = callScreeningRoleAvailable,
                onRequestPermission = onRequestPermission,
                onRequestCallScreeningRole = onRequestCallScreeningRole,
            )

            if (hasMissingPermission) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("申请所有缺少的运行时权限")
                }
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打开应用权限设置")
                }
            }

            SmsReceiptCard(latestReceipt)
        }
    }
}

@Composable
private fun PermissionRequirements(
    permissions: List<PermissionStatus>,
    callScreeningRoleGranted: Boolean,
    callScreeningRoleAvailable: Boolean,
    onRequestPermission: (AppPermission) -> Unit,
    onRequestCallScreeningRole: () -> Unit,
) {
    permissions.forEach { status ->
        PermissionCard(
            label = "${status.permission.stage} · ${status.permission.title}",
            description = status.permission.description,
            granted = status.granted,
            actionLabel = "申请此权限",
            onAction = { onRequestPermission(status.permission) },
        )
    }

    PermissionCard(
        label = "来电通知 · 来电识别角色",
        description = "允许系统在电话响铃前唤醒 RenKei。它不是普通权限，需要在单独的系统页面中确认。",
        granted = callScreeningRoleGranted,
        statusWhenMissing = if (callScreeningRoleAvailable) "未启用" else "设备不支持",
        actionLabel = "设置为来电识别应用",
        actionEnabled = callScreeningRoleAvailable,
        onAction = onRequestCallScreeningRole,
    )

    PermissionCard(
        label = "后端连接 · 网络访问",
        description = "INTERNET 与网络状态权限属于安装时权限，无需系统弹窗。",
        granted = true,
        statusWhenGranted = "已具备",
    )
}

@Composable
private fun SmsReceiptCard(latestReceipt: SmsReceipt?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("短信接收测试", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(latestReceipt.description())
        }
    }
}

private fun SmsReceipt?.description(): String =
    if (this == null) {
        "尚未收到测试短信。授权后，请用另一台设备给这台 Android 手机发一条普通 SMS。"
    } else {
        val time = DateFormat.getDateTimeInstance().format(Date(receivedAtEpochMillis))
        "最近在 $time 收到一条 $characterCount 字符的短信。正文没有写入本地存储。"
    }

@Preview(showBackground = true)
@Composable
private fun PermissionStatusScreenPreview() {
    RenKeiTheme {
        PermissionStatusScreen(
            permissions =
                AppPermission.entries.mapIndexed { index, permission ->
                    PermissionStatus(permission = permission, granted = index == 0)
                },
            callScreeningRoleGranted = false,
            callScreeningRoleAvailable = true,
            latestReceipt = null,
            onRequestPermission = {},
            onRequestPermissions = {},
            onRequestCallScreeningRole = {},
            onOpenAppSettings = {},
        )
    }
}
