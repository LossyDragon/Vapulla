package `in`.dragonbra.vapulla.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.fontFamily
import kotlinx.coroutines.flow.MutableStateFlow

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
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackPressed: (() -> Unit)? = null,
    toolbarText: String = stringResource(id = R.string.app_name),
    actions: @Composable RowScope.() -> Unit = {},
    searchTextState: MutableStateFlow<TextFieldValue>? = null,
    isSearching: Boolean = false,
    onSearchClose: (() -> Unit)? = null
) {
    Box {
        TopAppBar(
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)
            ),
            navigationIcon = {
                if (onBackPressed != null) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            },
            title = {
                Text(
                    text = toolbarText,
//                    fontFamily = fontFamily,
                    fontSize = 28.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        if (searchTextState != null) {
            AnimatedVisibility(
                visible = isSearching,
                enter = slideIn(),
                exit = slideUp()
            ) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                searchTextState.value = TextFieldValue("")
                                onSearchClose?.invoke()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                searchTextState.value = TextFieldValue("")
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                        }
                    },
                    title = {
                        SearchView(state = searchTextState)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchView(state: MutableStateFlow<TextFieldValue>) {
    val search = state.collectAsStateWithLifecycle()
    // TODO hide the bottom line on the text field?
    TextField(
        modifier = Modifier.height(64.dp),
        label = { Text("Search friends") },
        textStyle = TextStyle(fontSize = 18.sp),
        singleLine = true,
        shape = RectangleShape,
        value = search.value,
        onValueChange = { value ->
            state.value = value
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_VapullaToolbar() {
    val searchText: MutableStateFlow<TextFieldValue> = MutableStateFlow(TextFieldValue(""))
    var searchState by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        VapullaAppbar(
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_VapullaToolbar2() {
    val searchText: MutableStateFlow<TextFieldValue> = MutableStateFlow(TextFieldValue(""))
    var searchState by remember { mutableStateOf(true) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        VapullaAppbar(
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
