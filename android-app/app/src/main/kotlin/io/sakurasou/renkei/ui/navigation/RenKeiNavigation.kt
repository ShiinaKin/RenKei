@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package io.sakurasou.renkei.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.sakurasou.renkei.R
import io.sakurasou.renkei.ui.navigation.BottomDestination.HomeDestination
import io.sakurasou.renkei.ui.navigation.BottomDestination.PermissionsDestination
import io.sakurasou.renkei.ui.navigation.BottomDestination.SettingsDestination
import io.sakurasou.renkei.ui.screen.HomeRoute
import io.sakurasou.renkei.ui.screen.PermissionRoute
import io.sakurasou.renkei.ui.screen.SettingsRoute
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
private sealed class BottomDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val icon: Int,
) : NavKey {
    @Serializable
    data object HomeDestination : BottomDestination("HomeRoute", "主页", R.drawable.ic_home)

    @Serializable
    data object PermissionsDestination : BottomDestination("PermissionsRoute", "权限", R.drawable.ic_security)

    @Serializable
    data object SettingsDestination : BottomDestination("SettingRoute", "设置", R.drawable.ic_settings)
}

private val bottomDestinations: List<BottomDestination> =
    listOf(
        HomeDestination,
        PermissionsDestination,
        SettingsDestination,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenKeiNavigation(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val currentRoute = backStack.lastOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("RenKei") },
            )
        },
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
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<HomeDestination> { HomeRoute() }
                    entry<PermissionsDestination> { PermissionRoute() }
                    entry<SettingsDestination> { SettingsRoute() }
                },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { 0 }) togetherWith
                    slideOutHorizontally(targetOffsetX = { 0 })
            },
        )
    }
}
