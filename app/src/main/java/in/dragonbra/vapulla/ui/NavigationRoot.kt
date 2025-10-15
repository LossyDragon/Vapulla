package `in`.dragonbra.vapulla.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.ui.screens.login.LoginScreen
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import org.koin.androidx.compose.koinViewModel

data object RouteLogin

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    //val backStack = rememberNavBackStack(RouteLogin)
    val backStack = remember { mutableStateListOf<Any>(RouteLogin) }
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
                LoginScreen(viewModel = viewModel)
            }
        },
    )
}