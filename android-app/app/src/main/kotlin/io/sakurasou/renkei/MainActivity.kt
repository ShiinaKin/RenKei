@file:Suppress("ktlint:standard:function-naming")

package io.sakurasou.renkei

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.sakurasou.renkei.permission.PermissionStatus
import io.sakurasou.renkei.permission.permissionStatuses
import io.sakurasou.renkei.sms.SmsReceiptStore
import io.sakurasou.renkei.ui.navigation.RenKeiNavigation
import io.sakurasou.renkei.ui.screen.HomeScreen
import io.sakurasou.renkei.ui.screen.PermissionStatusScreen
import io.sakurasou.renkei.ui.theme.RenKeiTheme

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
@Suppress("LongMethod")
private fun RenKeiApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val roleManager = remember(context) { context.getSystemService(RoleManager::class.java) }

    var permissions by remember { mutableStateOf(permissionStatuses(context)) }
    var callScreeningRoleGranted by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }
    val callScreeningRoleAvailable =
        remember(roleManager) { roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }

    fun refreshPermissionState() {
        permissions = permissionStatuses(context)
        callScreeningRoleGranted = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshPermissionState()
        }
    val permissionBatchLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshPermissionState()
        }
    val roleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissionState()
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshPermissionState()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var latestReceipt by remember { mutableStateOf(SmsReceiptStore.latest(context)) }
    DisposableEffect(context) {
        val closeListener = SmsReceiptStore.listen(context) { latestReceipt = it }
        onDispose(closeListener)
    }

    RenKeiNavigation(
        homeScreen = {
            HomeScreen(
                latestReceipt = latestReceipt,
            )
        },
        permissionScreen = {
            PermissionStatusScreen(
                permissions = permissions,
                callScreeningRoleGranted = callScreeningRoleGranted,
                callScreeningRoleAvailable = callScreeningRoleAvailable,
                onRequestPermission = { permission ->
                    permissionLauncher.launch(permission.manifestName)
                },
                onRequestPermissions = {
                    val missingPermissions =
                        permissions
                            .filterNot(PermissionStatus::granted)
                            .map { it.permission.manifestName }
                            .toTypedArray()
                    if (missingPermissions.isNotEmpty()) permissionBatchLauncher.launch(missingPermissions)
                },
                onRequestCallScreeningRole = {
                    if (callScreeningRoleAvailable && !callScreeningRoleGranted) {
                        roleLauncher.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                        )
                    }
                },
                onOpenAppSettings = { openAppSettings(context) },
            )
        },
    )
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ),
    )
}
