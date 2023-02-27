package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    imageUrl: String?,
    appId: Int,
    gameName: String,
    hoursTwoWeeks: Int,
    hoursAllTime: Int,
    onOverflowClick: () -> Unit
) {
    val context = LocalContext.current
    val url by remember {
        val formattedUrl = imageUrl?.let {
            String.format(Utils.GAME_LOGO_URL, appId, imageUrl)
        }
        mutableStateOf(formattedUrl ?: "")
    }

    Box {
        ListItem(
            leadingContent = {
                CoilImage(
                    modifier = Modifier.size(58.dp),
                    imageRequest = {
                        ImageRequest.Builder(context)
                            .data(Utils.getAvatarUrl(url))
                            .placeholder(R.drawable.vapulla)
                            .crossfade(true)
                            .build()
                    },
                    previewPlaceholder = R.mipmap.ic_launcher_foreground,
                    imageOptions = ImageOptions(
                        requestSize = IntSize(58, 58)
                    )
                )
            },
            headlineText = {
                Text(gameName)
            },
            supportingText = {
                Column {
                    Text(
                        color = Color.White.copy(alpha = .50f),
                        fontSize = 12.sp,
                        text = stringResource(
                            id = R.string.textPlayedRecent,
                            formatPlayTime(hoursTwoWeeks)
                        )
                    )
                    Text(
                        color = Color.White.copy(alpha = .50f),
                        fontSize = 12.sp,
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
            imageUrl = null,
            appId = 0,
            gameName = "Team Fortress 2",
            hoursTwoWeeks = 60,
            hoursAllTime = 4140,
            onOverflowClick = {}
        )
    }
}
