package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@Composable
fun FriendListItem(
    avatarUrl: String,
    lastMessage: String,
    name: String,
    newMessageCount: String,
    nickName: String,
    status: String,
    time: String
) {
    val context = LocalContext.current

    Column {
        ListItem(
            headlineText = {
                Text(
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingText = {
                Text(
                    text = status,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Text(
                    text = time.subSequence(0, 7).toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RectangleShape,
                    color = Color.Green
                ) {
                    CoilImage(
                        modifier = Modifier.size(56.dp),
                        imageRequest = {
                            ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build()
                        },
                        previewPlaceholder = R.mipmap.ic_launcher_foreground,
                        imageOptions = ImageOptions(
                            requestSize = IntSize(56, 56)
                        )
                    )
                }
            }
        )
        Divider()
    }
}

@Preview
@Composable
private fun Preview_FriendListItem() {
    VapullaTheme {
        FriendListItem(
            avatarUrl = "",
            lastMessage = "Sup Bro",
            name = "CoolName",
            newMessageCount = "56",
            nickName = "Nickname",
            status = "Playing: Cool Game: 9000",
            time = "12:59"
        )
    }
}
