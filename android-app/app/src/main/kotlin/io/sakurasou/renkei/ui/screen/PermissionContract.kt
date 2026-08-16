package io.sakurasou.renkei.ui.screen

import io.sakurasou.renkei.permission.AppPermission
import io.sakurasou.renkei.permission.PermissionStatus

data class PermissionUiState(
    val permissions: List<PermissionStatus> = emptyList(),
    val callScreeningRoleGranted: Boolean = false,
    val callScreeningRoleAvailable: Boolean = false,
)

sealed interface PermissionAction {
    data class RequestPermission(
        val permission: AppPermission,
    ) : PermissionAction

    data object RequestAllPermissions : PermissionAction

    data object RequestCallScreeningRole : PermissionAction

    data object OpenAppSettings : PermissionAction
}
