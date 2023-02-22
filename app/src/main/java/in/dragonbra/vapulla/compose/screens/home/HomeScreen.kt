package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.*
import `in`.dragonbra.vapulla.compose.ui.theme.Shapes
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel) {

    val state by viewModel::homeState
    HomeScreenContent(
        state = state,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(state: HomeState) {
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() } // TODO Not used
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    Surface {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HomeScreenDrawer(drawerState)
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackBarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .shadow(elevation = 3.dp, shape = Shapes.medium)
                            .clickable { /*TODO*/ },
                        title = {
                            Text(
                                "Centered TopAppBar",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* doSomething() */ }) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Profile Settings"
                                )
                            }
                        }
                    )
                }
            ) { pv ->
                Text(modifier = Modifier.padding(pv), text = "Hello World!")
            }
        }
    }
}

@Composable
private fun HomeScreenDrawer(
    drawerState: DrawerState,
) {
    val scope = rememberCoroutineScope()
    val items = listOf("Online", "Away", "Invisible")
    val selectedItem = remember { mutableStateOf(items[0]) }

    val avatarUrl = Utils.getAvatarUrl("7e71a265059da00e5327c3f5b0a415e1c8625081")
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dominantColorState = rememberDominantColorState(
        defaultColor = MaterialTheme.colorScheme.surface
    ) { color ->
        color.contrastAgainst(surfaceColor) >= MinContrastOfPrimaryVsSurface
    }

    DynamicThemePrimaryColorsFromImage(dominantColorState) {
        LaunchedEffect(avatarUrl) {
            //dominantColorState.reset()
            dominantColorState.updateColorsFromImageUrl(avatarUrl)
        }

        ModalDrawerSheet(
            drawerContainerColor = Color.Transparent.copy(alpha = .50f),
            modifier = Modifier
                .fillMaxHeight()
                .verticalGradientScrim(
                    color = dominantColorState.color.copy(alpha = 0.50f),
                    startYPercentage = 1f,
                    endYPercentage = 0f
                )
        ) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(NavigationDrawerItemDefaults.ItemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(.25f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = LocalContext.current
                    CoilImage(
                        modifier = Modifier.size(150.dp),
                        imageRequest = {
                            ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build()
                        },
                        previewPlaceholder = R.drawable.vapulla,
                        imageOptions = ImageOptions(
                            requestSize = IntSize(150, 150)
                        )
                    )

                    Text("Some Cool Name")
                }

                Spacer(Modifier.height(36.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        icon = {
                            val tint = if (item == selectedItem.value) Color.Black else Color.Gray
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = tint
                            )
                        },
                        label = { Text(item) },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem.value = item
                        }
                    )
                }

                Divider(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Add Freind", fontSize = 18.sp)
                        }
                    }
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Settings", fontSize = 18.sp)
                        }
                    }
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Log Out", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_HomeScreenContent() {
    val state = HomeState()

    VapullaTheme {
        HomeScreenContent(state = state)
    }
}

@Preview
@Composable
private fun Preview_HomeScreenDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    VapullaTheme {
        HomeScreenDrawer(drawerState = drawerState)
    }
}