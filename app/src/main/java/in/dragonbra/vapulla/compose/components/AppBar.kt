package `in`.dragonbra.vapulla.compose.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.screens.home.FriendItem
import `in`.dragonbra.vapulla.compose.ui.theme.fontFamily
import `in`.dragonbra.vapulla.model.FriendListItem
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
fun HomeAppbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
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
                Box(modifier = Modifier.size(90.0.dp)) { }
            },
            title = {
                Text(
                    text = toolbarText,
                    fontFamily = fontFamily,
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
                    fontFamily = fontFamily,
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
private fun Preview_HomeToolbar() {
    val searchText: MutableStateFlow<TextFieldValue> = MutableStateFlow(TextFieldValue(""))
    var searchState by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column {
            HomeAppbar(
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
            FriendItem(
                friend = FriendListItem(
                    id = 0,
                    lastMessage = "Left 4 Dead 2 is so fun!",
                    name = "New Friend Request",
                    nickname = "New Friend Request",
                    relation = EFriendRelationship.RequestRecipient.code(),
                    state = EPersonaState.Offline.code()
                ),
                onClickChat = {},
                onClickProfile = {}
            )
        }
    }
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
