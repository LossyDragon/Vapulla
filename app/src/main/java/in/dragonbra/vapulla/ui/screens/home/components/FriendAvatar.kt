package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils

/**
 * Avatar with Material 3 Expressive rounding. Presence is conveyed by the row's name colour
 * and status line, so the avatar itself stays clean.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FriendAvatar(modifier: Modifier = Modifier, size: Dp = 52.dp, friend: SteamFriend) {
    CoilImage(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.large),
        imageModel = { Utils.getAvatarURL(friend.avatar) },
        imageOptions = ImageOptions(
            contentScale = ContentScale.Crop,
            contentDescription = "Avatar for ${friend.name}",
        ),
        previewPlaceholder = painterResource(R.drawable.vapulla_background),
        loading = { LoadingIndicator() },
        failure = {
            Image(
                painter = painterResource(R.drawable.vapulla),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            FriendAvatar(
                size = 128.dp,
                friend = SteamFriend(
                    id = 0,
                    name = "Vapulla",
                    gameAppID = 440,
                    state = EPersonaState.Away,
                ),
            )
        }
    }
}
