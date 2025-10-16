package `in`.dragonbra.vapulla.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.service.ServiceManager
import `in`.dragonbra.vapulla.ui.screens.home.HomeScreen
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginScreen
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

data object RouteLogin
data object RouteHome

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val serviceManager = getKoin().get<ServiceManager>()

    val backStack = remember {
        val route = if (serviceManager.isLoggedIn.value) RouteHome else RouteLogin
        mutableStateListOf(route)
    }
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategy = SinglePaneSceneStrategy(),
        entryProvider = entryProvider {
            entry<RouteLogin> {
                val viewModel = koinViewModel<LoginViewModel>()
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        backStack.clear()
                        backStack.add(RouteHome)
                    }
                )
            }
            entry<RouteHome> {
                val viewModel = koinViewModel<HomeViewModel>()
                HomeScreen(viewModel = viewModel)
            }
        },
    )
}