package `in`.dragonbra.vapulla.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.ui.composables.navigation.LogOutDialog
import `in`.dragonbra.vapulla.ui.composables.navigation.NavigationDrawer
import `in`.dragonbra.vapulla.ui.screens.web.LocalExternalUriHandler
import `in`.dragonbra.vapulla.ui.screens.web.SteamPages
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

/**
 * Keeps the back stack when the activity is recreated, such as when the phone is rotated.
 * The routes are stored as they are, which is why [Routes] is serializable.
 */
private val BackStackSaver = listSaver<SnapshotStateList<Routes>, Routes>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

/**
 * Makes sign-in the only screen on the stack. The sign-in screen already on it is kept, so
 * there is never more than one, and the stack is never empty on the way.
 */
private fun SnapshotStateList<Routes>.returnToLogin() {
    if (Routes.Login !in this) add(0, Routes.Login)
    removeAll { it != Routes.Login }
}

/**
 * Hosts the navigation drawer and the single-pane back stack of [Routes]. The screens
 * themselves are in NavigationEntries.kt.
 *
 * @param openScreen the screen a tapped notification asks for: a chat, a profile or Downloads.
 * @param onScreenOpened called once that screen is open, so it is not opened again.
 */
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    openScreen: Routes? = null,
    onScreenOpened: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val koin = getKoin()
    val connection = remember(koin) { koin.get<SteamServiceConnection>() }
    val scope = rememberCoroutineScope()

    val backStack = rememberSaveable(saver = BackStackSaver) {
        // Skip the login screen when the service is already signed in (e.g. after the
        // activity was recreated while the foreground service kept running).
        val route = if (SteamService.isLoggedIn.value) Routes.Home else Routes.Login
        mutableStateListOf(route)
    }

    // With the setting on, Steam's community and store pages open inside the app, signed
    // in. Every other address, and every address with the setting off, goes to the browser.
    val browser = LocalUriHandler.current
    val signedInWebPages = LocalAppSettings.current.signedInWebPages
    val uriHandler = remember(browser, signedInWebPages) {
        object : UriHandler {
            override fun openUri(uri: String) {
                if (signedInWebPages && SteamPages.isSignedInPage(uri)) {
                    backStack.add(Routes.WebPage(uri))
                } else {
                    browser.openUri(uri)
                }
            }
        }
    }

    val onBack: () -> Unit = { backStack.removeLastOrNull() }
    val onNavDrawerAction: () -> Unit = {
        scope.launch {
            drawerState.apply {
                if (isClosed) open() else close()
            }
        }
    }

    // The session decides what may be on the stack. Signed in, anything. In an anonymous
    // session, which is started from the sign-in screen, Under the Hood goes on top of it
    // and is all there is to show. With neither, only sign-in: after logging out, once a
    // dropped connection could not be re-established, or when the anonymous session ends.
    //
    // The first values arrive when the activity is recreated too, and must leave a stack
    // that fits the session as it is.
    LaunchedEffect(Unit) {
        combine(SteamService.isLoggedIn, SteamService.isAnonymous) { loggedIn, anonymous ->
            loggedIn to anonymous
        }
            .collect { (loggedIn, anonymous) ->
                when {
                    loggedIn -> Unit

                    anonymous -> if (backStack.lastOrNull() == Routes.Login) {
                        backStack.add(Routes.UnderTheHood)
                    }

                    else -> backStack.returnToLogin()
                }
            }
    }

    // A tapped notification asks for a screen. Home being on the stack means we are signed
    // in; until then (the app was started by the tap and is still signing in) the request
    // waits, and is carried out as soon as Home appears.
    val signedIn = Routes.Home in backStack
    LaunchedEffect(openScreen, signedIn) {
        if (openScreen != null && signedIn) {
            if (backStack.lastOrNull() != openScreen) {
                // Back from it leads to the friends list, whatever was open before.
                backStack.removeAll { it != Routes.Home }
                backStack.add(openScreen)
            }
            drawerState.close()
            onScreenOpened()
        }
    }

    LaunchedEffect(backStack.lastOrNull()) {
        // Ensure the Nav Drawer is closed on Login.
        if (backStack.lastOrNull() == Routes.Login) {
            drawerState.close()
        }
    }

    // An anonymous session has only Under the Hood to show, and no drawer to get lost in.
    val anonymous by SteamService.isAnonymous.collectAsStateWithLifecycle()

    // Log Out in the drawer asks first, and whether to forget the saved sign-in as well.
    var showLogOutDialog by rememberSaveable { mutableStateOf(false) }
    if (showLogOutDialog) {
        LogOutDialog(
            onConfirm = { clearCredentials ->
                showLogOutDialog = false
                if (clearCredentials) {
                    connection.steamService?.logOut()
                } else {
                    connection.steamService?.disconnect()
                }
            },
            onDismiss = { showLogOutDialog = false },
        )
    }

    CompositionLocalProvider(
        LocalUriHandler provides uriHandler,
        LocalExternalUriHandler provides browser,
    ) {
        ModalNavigationDrawer(
            // Not on a web page either: a swipe there belongs to the page, to scroll it sideways.
            gesturesEnabled = !anonymous &&
                backStack.last().let { it != Routes.Login && it !is Routes.WebPage },
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawer(
                    currentSelection = backStack.last(),
                    onLogOut = { showLogOutDialog = true },
                    onNavigationClick = { route ->
                        // Home is the root of the stack, so going there only pops back to it.
                        backStack.removeAll { it != Routes.Home }
                        if (route != Routes.Home || backStack.isEmpty()) {
                            backStack.add(route)
                        }
                        scope.launch {
                            drawerState.close()
                        }
                    },
                )
            },
            content = {
                NavDisplay(
                    modifier = modifier,
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        friendsEntries(backStack, onBack, onNavDrawerAction)
                        libraryEntries(backStack, onBack, onNavDrawerAction)
                        accountEntries(backStack, onBack, onNavDrawerAction)
                        underHoodEntries(
                            backStack = backStack,
                            onBack = onBack,
                            onNavDrawerAction = onNavDrawerAction,
                            // Keeps a saved sign-in: only the anonymous session ends.
                            onLeaveAnonymous = {
                                connection.steamService?.logOut(clearCredentials = false)
                            },
                        )
                    },
                )
            },
        )
    }
}
