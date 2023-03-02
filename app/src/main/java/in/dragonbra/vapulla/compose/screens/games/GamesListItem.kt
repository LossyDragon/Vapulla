package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.formatPlayTime
import `in`.dragonbra.vapulla.util.Utils

@Composable
fun GamesListItem(
    appId: Int,
    gameName: String,
    hoursTwoWeeks: Int,
    hoursAllTime: Int,
    onOverflowClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val width = with(density) { 460.toDp() }
        val height = with(density) { 215.toDp() }

        ListItem(
            leadingContent = {
                CoilImage(
                    modifier = Modifier.size(width, height),
                    imageRequest = {
                        ImageRequest.Builder(context)
                            .data(String.format(Utils.GAME_LOGO_URL, appId))
                            .crossfade(true)
                            .build()
                    },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Fit,
                        // requestSize = IntSize(460, 215)
                    ),
                    previewPlaceholder = R.mipmap.ic_launcher_foreground,
                    loading = {
                        Box(
                            modifier = Modifier.size(width, height),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    failure = {
                        Box(
                            modifier = Modifier.size(width, height),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            headlineText = {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = gameName
                )
            },
            supportingText = {
                Column {
                    Text(
                        color = Color.White.copy(alpha = .50f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = stringResource(
                            id = R.string.textPlayedRecent,
                            formatPlayTime(hoursTwoWeeks)
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
                IconButton(onClick = onOverflowClick) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
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
            onOverflowClick = {}
        )
    }
}
