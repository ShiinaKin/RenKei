@file:Suppress("ktlint:standard:function-naming")

package io.sakurasou.renkei.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.sakurasou.renkei.R
import io.sakurasou.renkei.ui.navigation.BottomDestination.PermissionsRoute
import io.sakurasou.renkei.ui.navigation.BottomDestination.SettingsRoute
import io.sakurasou.renkei.ui.screen.SettingsScreen
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
private sealed class BottomDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val icon: Int,
) : NavKey {
    @Serializable
    data object SettingsRoute : BottomDestination("SettingsRoute", "设置", R.drawable.ic_settings)

    @Serializable
    data object PermissionsRoute : BottomDestination("PermissionsRoute", "权限", R.drawable.ic_security)
}

private val bottomDestinations: List<BottomDestination> =
    listOf(
        SettingsRoute,
        PermissionsRoute,
    )

@Composable
fun RenKeiNavigation(
    modifier: Modifier = Modifier,
    permissionScreen: @Composable () -> Unit,
) {
    val backStack = rememberNavBackStack(SettingsRoute)
    val currentRoute = backStack.lastOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination,
                        onClick = {
                            if (currentRoute != destination) {
                                backStack.add(destination)
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            onBack = { backStack.removeLastOrNull() },
            entryProvider =
                entryProvider {
                    entry<SettingsRoute> { SettingsScreen() }
                    entry<PermissionsRoute> { permissionScreen() }
                },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { (it * 0.1).roundToInt() }) togetherWith
                    slideOutHorizontally(targetOffsetX = { (it * 0.1).roundToInt() })
            },
        )
    }
}
