package `in`.dragonbra.vapulla.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val friendsList by viewModel.friendsList.collectAsState()
    HomeScreenContent(
        friendsList = friendsList
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    friendsList: List<FriendListItem>
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier,
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = {
                if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {},
                        text = "Search",
                        textAlign = TextAlign.Center,
                    )
                }
            },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("Back") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                            content = {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        )
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Drawer Title",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )

                    HorizontalDivider()

                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Friends") },
                        icon = { Icon(Icons.Outlined.Groups, null) },
                        selected = true,
                        onClick = { TODO() }
                    )

                    NavigationDrawerItem(
                        label = { Text("Library") },
                        icon = { Icon(Icons.Outlined.Games, null) },
                        selected = false,
                        onClick = { TODO() }
                    )

                    NavigationDrawerItem(
                        label = { Text("Downloads") },
                        icon = { Icon(Icons.Outlined.Download, null) },
                        selected = false,
                        onClick = { TODO() }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Outlined.Settings, null) },
                        selected = false,
                        onClick = { TODO() }
                    )

                    NavigationDrawerItem(
                        label = { Text("Log Out") },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                        selected = false,
                        onClick = { TODO() }
                    )
                }
            }
        },
    ) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                AppBarWithSearch(
                    scrollBehavior = scrollBehavior,
                    state = searchBarState,
                    inputField = inputField,
                    navigationIcon = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = { PlainTooltip { Text("Menu") } },
                            state = rememberTooltipState(),
                            content = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.apply {
                                                if (isClosed) open() else close()
                                            }
                                        }
                                    },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu"
                                        )
                                    }
                                )
                            }
                        )
                    },
                    actions = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = { PlainTooltip { Text("Account") } },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { TODO() }) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Account",
                                )
                            }
                        }
                    },
                    colors = appBarWithSearchColors,
                )

                ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
                    Text("TODO THING")
                    // TODO()
                }
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    content = {
                        items(friendsList, key = { it.id }) { friend ->
                            ListItem(
                                modifier = Modifier
                                    .animateItem()
                                    .fillParentMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 0.dp),
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    headlineColor = friend.statusColor,
                                    supportingColor =  friend.statusColor,
                                ),
                                headlineContent = {
                                    Text(
                                        text = buildAnnotatedString {
                                            append(friend.nameOrNickname)
                                            if (friend.nickname.orEmpty().isNotEmpty()) {
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                    ),
                                                ) {
                                                    append(" * ")
                                                }
                                            } else {
                                                append(" ")
                                            }
                                            appendInlineContent("icon", "[icon]")
                                        },
                                        inlineContent = mapOf(
                                            "icon" to InlineTextContent(
                                                Placeholder(
                                                    width = 14.sp,
                                                    height = 14.sp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                                ),
                                                children = {
                                                    friend.statusIcon?.let {
                                                        Icon(
                                                            imageVector = it,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            contentDescription = it.name,
                                                        )
                                                    }
                                                },
                                            ),
                                        ),
                                    )
                                },
                                supportingContent = {
                                    Text(text = friend.isPlayingGameName)
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = friend.statusColor,
                                                shape = MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center,
                                        content = {
                                            AsyncImage(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(MaterialTheme.shapes.small),
                                                model = Utils.getAvatarURL(friend.avatar),
                                                contentDescription = "Avatar for ${friend.name}",
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(R.drawable.vapulla),
                                                error = painterResource(R.drawable.vapulla)
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                )
            }
        )
    }
}

@Preview(
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PReview() {
    val friendsList = List(10) {
        FriendListItem(
            id = it.toLong(),
            name = "Friend Name $it",
            avatar = null,
            relation = EFriendRelationship.Friend.code(),
            state = EPersonaState.from(1).code(),
            gameAppId = 440,
            gameName = "Team Fortress 2",
            lastLogOn = 0,
            lastLogOff = 0,
            stateFlags = EPersonaStateFlag.code(EPersonaStateFlag.from(2048)),
            typingTs = 0,
            lastMessage = "Beans are yummy!",
            lastMessageTime = 0,
            newMessageCount = it,
            nickname = "Nick Name $it",
        )
    }
    VapullaTheme {
        HomeScreenContent(
            friendsList = friendsList
        )
    }
}