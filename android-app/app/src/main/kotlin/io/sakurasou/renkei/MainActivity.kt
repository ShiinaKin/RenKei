@file:Suppress("FunctionNaming")

package io.sakurasou.renkei

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.sakurasou.renkei.sms.SmsReceipt
import io.sakurasou.renkei.sms.SmsReceiptStore
import io.sakurasou.renkei.ui.theme.RenKeiTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RenKeiTheme {
                RenKeiApp()
            }
        }
    }
}

@Composable
private fun RenKeiApp() {
    val context = LocalContext.current
    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            smsPermissionGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
            notificationPermissionGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

    var latestReceipt by remember { mutableStateOf(SmsReceiptStore.latest(context)) }
    DisposableEffect(context) {
        val closeListener = SmsReceiptStore.listen(context) { latestReceipt = it }
        onDispose(closeListener)
    }

    RelayStatusScreen(
        smsPermissionGranted = smsPermissionGranted,
        notificationPermissionGranted = notificationPermissionGranted,
        latestReceipt = latestReceipt,
        onRequestPermissions = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
        },
    )
}

@Composable
private fun RelayStatusScreen(
    smsPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    latestReceipt: SmsReceipt?,
    onRequestPermissions: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(24.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("RenKei", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Android 端负责捕获系统事件，并在后续步骤中可靠地发送给你的后端。",
                style = MaterialTheme.typography.bodyLarge,
            )

            PermissionCard("接收短信", smsPermissionGranted)
            PermissionCard("显示调试通知", notificationPermissionGranted)

            if (!smsPermissionGranted || !notificationPermissionGranted) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("授予第一阶段权限")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("短信接收测试", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(latestReceipt.description())
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    label: String,
    granted: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                if (granted) "已授权" else "未授权",
                color =
                    if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }
}

private fun SmsReceipt?.description(): String =
    if (this == null) {
        "尚未收到测试短信。授权后，请用另一台设备给这台 Android 手机发一条短信。"
    } else {
        val time = DateFormat.getDateTimeInstance().format(Date(receivedAtEpochMillis))
        "最近在 $time 收到一条 $characterCount 字符的短信。正文没有写入本地存储。"
    }

@Preview(showBackground = true)
@Composable
private fun RelayStatusScreenPreview() {
    RenKeiTheme {
        RelayStatusScreen(
            smsPermissionGranted = true,
            notificationPermissionGranted = false,
            latestReceipt = null,
            onRequestPermissions = {},
        )
    }
}
