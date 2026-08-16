@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package io.sakurasou.renkei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import io.sakurasou.renkei.ui.navigation.RenKeiNavigation
import io.sakurasou.renkei.ui.theme.RenKeiTheme

@AndroidEntryPoint
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
    RenKeiNavigation()
}
