package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VapullaToolbar(
    drawerState: DrawerState? = null,
    toolbarText: String = stringResource(id = R.string.app_name),
    actions: @Composable RowScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    TopAppBar(
        modifier = Modifier.shadow(elevation = 3.dp),
        title = {
            Text(
                text = toolbarText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
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
}

@Preview
@Composable
private fun Preview_VapullaToolbar() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    MaterialTheme(colorScheme = darkColorScheme()) {
        VapullaToolbar(
            drawerState = drawerState,
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.SortByAlpha,
                        contentDescription = "Search"
                    )
                }
            }
        )
    }
}
