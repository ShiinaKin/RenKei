@file:Suppress("FunctionNaming")

package io.sakurasou.renkei.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {}
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
