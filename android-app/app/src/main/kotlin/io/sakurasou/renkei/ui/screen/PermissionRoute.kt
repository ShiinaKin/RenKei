@file:Suppress("FunctionNaming")

package io.sakurasou.renkei.ui.screen

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.sakurasou.renkei.ui.viewmodel.PermissionViewModel

@Composable
@Suppress("LongMethod")
fun PermissionRoute(viewModel: PermissionViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val roleManager = remember(context) { context.getSystemService(RoleManager::class.java) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refresh()
        }
    val permissionBatchLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.refresh()
        }
    val roleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.refresh()
        }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionStatusScreen(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is PermissionAction.RequestPermission -> {
                    permissionLauncher.launch(action.permission.manifestName)
                }

                PermissionAction.RequestAllPermissions -> {
                    val missingPermissions =
                        uiState.permissions
                            .filterNot { it.granted }
                            .map { it.permission.manifestName }
                            .toTypedArray()
                    if (missingPermissions.isNotEmpty()) {
                        permissionBatchLauncher.launch(missingPermissions)
                    }
                }

                PermissionAction.RequestCallScreeningRole -> {
                    if (uiState.callScreeningRoleAvailable && !uiState.callScreeningRoleGranted) {
                        roleLauncher.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                        )
                    }
                }

                PermissionAction.OpenAppSettings -> {
                    openAppSettings(context)
                }
            }
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
