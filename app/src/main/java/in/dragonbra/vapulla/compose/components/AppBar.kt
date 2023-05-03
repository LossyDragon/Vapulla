package `in`.dragonbra.vapulla.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.R
import java.lang.RuntimeException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Animation slide in when searching is enabled
private val slideIn = {
    slideIn(
        initialOffset = { IntOffset(it.width, 0) },
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    )
}

// Animation slide up when searching is disabled
private val slideUp = {
    slideOut(
        targetOffset = { IntOffset(0, -it.height) },
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VapullaAppbar(
    isScrolled: State<Boolean> = derivedStateOf { false },
    drawerState: DrawerState? = null,
    onBackPressed: (() -> Unit)? = null,
    toolbarText: String = stringResource(id = R.string.app_name),
    actions: @Composable RowScope.() -> Unit = {},
    searchTextState: MutableStateFlow<TextFieldValue>? = null,
    isSearching: Boolean = false,
    onSearchClose: (() -> Unit)? = null
) {
    val topBarContainerColor = if (isScrolled.value) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box {
        TopAppBar(
            // modifier = Modifier.shadow(elevation = 3.dp),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarContainerColor,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            title = {
                Text(
                    text = toolbarText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (onBackPressed != null && drawerState != null) {
                    throw RuntimeException("Navigation Icon can only have one item")
                }

                val scope = rememberCoroutineScope()

                onBackPressed?.let {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
                drawerState?.let {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Toggle Drawer Menu"
                        )
                    }
                }
            },
            actions = actions
        )

        if (searchTextState != null) {
            AnimatedVisibility(
                visible = isSearching,
                enter = slideIn(),
                exit = slideUp()
            ) {
                SearchView(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    state = searchTextState,
                    onClose = { onSearchClose?.invoke() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchView(
    backgroundColor: Color,
    state: MutableStateFlow<TextFieldValue>,
    onClose: () -> Unit
) {
    Box(Modifier.background(backgroundColor)) {
        TextField(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets
                        .statusBars
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                .fillMaxWidth()
                .requiredHeight(64.dp),
            label = { Text("Search apps...") },
            textStyle = TextStyle(fontSize = 18.sp),
            singleLine = true,
            shape = RectangleShape,
            value = state.collectAsState().value,
            onValueChange = { value ->
                state.value = value
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        onClose()
                        state.value = TextFieldValue("")
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        // Remove text from TextField when you press the 'X' icon
                        state.value = TextFieldValue("")
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview_VapullaToolbar() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val searchText: MutableStateFlow<TextFieldValue> = MutableStateFlow(TextFieldValue(""))
    var searchState by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        VapullaAppbar(
            drawerState = drawerState,
            actions = {
                IconButton(onClick = { searchState = true }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
            },
            searchTextState = searchText,
            isSearching = searchState,
            onSearchClose = { searchState = false }
        )
    }
}
