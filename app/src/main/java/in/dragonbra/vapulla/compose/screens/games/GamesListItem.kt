package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.GameImage
import `in`.dragonbra.vapulla.compose.util.formatPlayTime

@Composable
fun GamesListItem(
    appId: Int,
    gameName: String,
    hoursTwoWeeks: Int?,
    hoursAllTime: Int,
    onItemClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        ListItem(
            leadingContent = {
                GameImage(appId = appId)
            },
            headlineContent = {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = gameName
                )
            },
            supportingContent = {
                Column {
                    Text(
                        color = Color.White.copy(alpha = .50f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = stringResource(
                            id = R.string.textPlayedRecent,
                            formatPlayTime(hoursTwoWeeks ?: 0)
                        )
                    )
                    Text(
                        color = Color.White.copy(alpha = .50f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = stringResource(
                            id = R.string.textPlayedForever,
                            formatPlayTime(hoursAllTime)
                        )
                    )
                }
            },
            trailingContent = {
                IconButton(onClick = onItemClick) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null)
                }
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

@Preview
@Composable
private fun Preview_GamesListItem() {
    VapullaTheme {
        GamesListItem(
            appId = 440,
            gameName = "Team Fortress 2",
            hoursTwoWeeks = 60,
            hoursAllTime = 4140,
            onItemClick = {}
        )
    }
}
