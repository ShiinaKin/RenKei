@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package io.sakurasou.renkei.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.sakurasou.renkei.ui.component.AppMessageDialog
import io.sakurasou.renkei.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )

    uiState.message?.let { message ->
        AppMessageDialog(
            message = message,
            isError = uiState.isMessageError,
            onDismiss = { viewModel.onAction(SettingsAction.DismissMessage) },
        )
    }
}
