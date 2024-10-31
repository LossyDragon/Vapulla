package `in`.dragonbra.vapulla.compose.util

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.CoilImageState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.icons.VR
import `in`.dragonbra.vapulla.model.FriendListItem

/**
 * Helper functions that relate to Images/Icons
 */
fun getStatusIcon(friend: FriendListItem?): ImageVector? {
    val flags = EPersonaStateFlag.from(friend?.stateFlags ?: 0)
    return when {
        friend?.isRequestRecipient == true -> Icons.Default.PersonAddAlt1
        friend?.isAwayOrSnooze == true -> Icons.Default.Bedtime
        flags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
        flags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
        flags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
        flags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
        else -> null
    }
}

@Composable
fun StaticImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    loading: @Composable BoxScope.(CoilImageState.Loading) -> Unit = {},
    failure: @Composable BoxScope.(CoilImageState.Failure) -> Unit = {}
) {
    val context = LocalContext.current
    CoilImage(
        modifier = modifier,
        imageRequest = {
            ImageRequest.Builder(context)
                .data(url)
                .placeholder(R.drawable.vapulla)
                .crossfade(true)
                .build()
        },
        previewPlaceholder = painterResource(id = R.mipmap.ic_launcher_foreground),
        imageOptions = ImageOptions(alignment = alignment, contentScale = contentScale),
        loading = loading,
        failure = failure
    )
}

// Modifier.size(150.dp)
@Composable
fun StickerImage(
    modifier: Modifier = Modifier,
    url: String
) {
    val context = LocalContext.current
    CoilImage(
        modifier = modifier,
        imageRequest = {
            ImageRequest.Builder(context)
                .data(url)
                .placeholder(R.drawable.vapulla)
                .crossfade(true)
                .build()
        },
        previewPlaceholder = painterResource(id = R.mipmap.ic_launcher_foreground),
        imageOptions = ImageOptions(contentScale = ContentScale.Fit)
    )
}
