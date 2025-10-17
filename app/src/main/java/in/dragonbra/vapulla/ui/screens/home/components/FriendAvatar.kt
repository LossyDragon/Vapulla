package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FriendAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    friend: SteamFriend,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        content = {
            CoilImage(
                modifier = modifier.size(size = size),
                imageModel = { Utils.getAvatarURL(friend.avatar) },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = "Avatar for ${friend.name}"
                ),
                previewPlaceholder = painterResource(R.drawable.vapulla_background),
                loading = { LoadingIndicator() },
                failure = {
                    Image(
                        painter = painterResource(R.drawable.vapulla),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )

            // Basically a 'VerticalDivider' but height constrained.
            Canvas(Modifier.size(height =size, width = 4.dp)) {
                drawLine(
                    color = friend.statusColor,
                    pathEffect = if (friend.isAwayOrSnooze) {
                        val heightPx = this@Canvas.size.height
                        val numberOfDashes = 7
                        val numberOfGaps = numberOfDashes - 1
                        val totalSegments = numberOfDashes + numberOfGaps
                        val segmentLength = heightPx / totalSegments

                        PathEffect.dashPathEffect(
                            intervals = floatArrayOf(segmentLength, segmentLength)
                        )
                    } else {
                        null
                    },
                    strokeWidth = 4.dp.toPx(),
                    start = Offset(4.dp.toPx() / 2, 0f),
                    end = Offset(4.dp.toPx() / 2, this@Canvas.size.height),
                )
            }
        }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    val friend =
        SteamFriend(id = 0, name = "Vapulla", gameAppId = 440, state = EPersonaState.Away)

    VapullaTheme {
        Surface {
            FriendAvatar(
                size = 128.dp,
                friend = friend
            )
        }
    }
}