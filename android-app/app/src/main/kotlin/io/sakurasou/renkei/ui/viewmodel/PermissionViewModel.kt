package io.sakurasou.renkei.ui.viewmodel

import android.app.role.RoleManager
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sakurasou.renkei.permission.permissionStatuses
import io.sakurasou.renkei.ui.screen.PermissionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val roleManager = context.getSystemService(RoleManager::class.java)
        private val _uiState = MutableStateFlow(createUiState())

        val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

        fun refresh() {
            _uiState.value = createUiState()
        }

        private fun createUiState(): PermissionUiState =
            PermissionUiState(
                permissions = permissionStatuses(context),
                callScreeningRoleGranted = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
                callScreeningRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING),
            )
    }
