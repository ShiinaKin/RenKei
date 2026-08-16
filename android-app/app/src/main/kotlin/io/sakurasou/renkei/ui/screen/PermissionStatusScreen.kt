@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package io.sakurasou.renkei.ui.screen

import android.annotation.SuppressLint
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
import io.sakurasou.renkei.ui.component.PermissionCard
import io.sakurasou.renkei.ui.theme.RenKeiTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PermissionStatusScreen(
    uiState: PermissionUiState,
    onAction: (PermissionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMissingPermission = uiState.permissions.any { !it.granted }

    Scaffold(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxSize(),
        ) {
            Text(
                "这里集中准备短信转发和来电通知所需的 Android 权限。所有申请都由你主动触发。",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            PermissionRequirements(
                permissions = uiState.permissions,
                callScreeningRoleGranted = uiState.callScreeningRoleGranted,
                callScreeningRoleAvailable = uiState.callScreeningRoleAvailable,
                onAction = onAction,
            )

            if (hasMissingPermission) {
                Button(
                    onClick = { onAction(PermissionAction.RequestAllPermissions) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("申请所有缺少的运行时权限")
                }
                OutlinedButton(
                    onClick = { onAction(PermissionAction.OpenAppSettings) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打开应用权限设置")
                }
            }
        }
    }
}

@Composable
private fun PermissionRequirements(
    permissions: List<PermissionStatus>,
    callScreeningRoleGranted: Boolean,
    callScreeningRoleAvailable: Boolean,
    onAction: (PermissionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        permissions.forEach { status ->
            PermissionCard(
                label = "${status.permission.stage} · ${status.permission.title}",
                description = status.permission.description,
                granted = status.granted,
                actionLabel = "申请此权限",
                onAction = { onAction(PermissionAction.RequestPermission(status.permission)) },
            )
        }

        PermissionCard(
            label = "来电通知 · 来电识别角色",
            description = "允许系统在电话响铃前唤醒 RenKei。它不是普通权限，需要在单独的系统页面中确认。",
            granted = callScreeningRoleGranted,
            statusWhenMissing = if (callScreeningRoleAvailable) "未启用" else "设备不支持",
            actionLabel = "设置为来电识别应用",
            actionEnabled = callScreeningRoleAvailable,
            onAction = { onAction(PermissionAction.RequestCallScreeningRole) },
        )

        PermissionCard(
            label = "后端连接 · 网络访问",
            description = "INTERNET 与网络状态权限属于安装时权限，无需系统弹窗。",
            granted = true,
            statusWhenGranted = "已具备",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionStatusScreenPreview() {
    RenKeiTheme {
        PermissionStatusScreen(
            uiState =
                PermissionUiState(
                    permissions =
                        AppPermission.entries.mapIndexed { index, permission ->
                            PermissionStatus(permission = permission, granted = index == 0)
                        },
                    callScreeningRoleGranted = false,
                    callScreeningRoleAvailable = true,
                ),
            onAction = {},
        )
    }
}
