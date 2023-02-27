package `in`.dragonbra.vapulla.compose.util

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import java.math.RoundingMode
import java.text.DecimalFormat

private const val ALL_ZEROS = "0000000000000000000000000000000000000000"
private const val STEAM_CDN = "https://cdn.akamai.steamstatic.com"
private const val STEAM_AVATAR = "steamcommunity/public/images/avatars"
private const val AVATAR_URL = "${STEAM_CDN}/${STEAM_AVATAR}/"
private const val DEFAULT_AVATAR =
    "$AVATAR_URL/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

fun getAvatarUrl(avatar: String?): String {
    if (avatar.isNullOrEmpty() || avatar == ALL_ZEROS) {
        return DEFAULT_AVATAR
    }

    return "${AVATAR_URL}${avatar.substring(0, 2)}/${avatar}_full.jpg"
}

fun formatPlayTime(time: Int): Double {
    return DecimalFormat("#.#").run {
        roundingMode = RoundingMode.CEILING
        return@run time.div(60f).toDouble()
    }
}

@Composable
fun getStatusText(friend: FriendListItem?): String {
    if (friend == null) {
        return stringResource(id = R.string.statusOfflineLabel)
    }

    if (friend.gameAppId != 0 || !friend.gameName.isNullOrEmpty()) {
        return stringResource(R.string.statusPlaying, friend.gameName ?: "")
    }

    val currentTime = System.currentTimeMillis()
    val resolution = DateUtils.MINUTE_IN_MILLIS
    val relativeDate =
        DateUtils.getRelativeTimeSpanString(friend.lastLogOff, currentTime, resolution)

    return when (EPersonaState.from(friend.state ?: 0)) {
        EPersonaState.Online -> stringResource(R.string.statusOnline)
        EPersonaState.Busy -> stringResource(R.string.statusBusy)
        EPersonaState.Away -> stringResource(R.string.statusAway)
        EPersonaState.Snooze -> stringResource(R.string.statusSnooze)
        EPersonaState.LookingToTrade -> stringResource(R.string.statusLookingTrade)
        EPersonaState.LookingToPlay -> stringResource(R.string.statusLookingPlay)
        else -> stringResource(R.string.statusOffline, relativeDate)
    }
}

@Composable
fun friendNameBuilder(friend: FriendListItem?): AnnotatedString {
    val builder = AnnotatedString.Builder()

    val color = getStatusColor(friend)
    builder.pushStyle(SpanStyle(color = color))

    if (friend == null) {
        builder.append("Loading...")
        return builder.toAnnotatedString()
    }

    if (!friend.hasNickname()) {
        builder.append(friend.friendName)
        return builder.toAnnotatedString()
    }

    builder.append(friend.friendName)
    builder.pushStyle(SpanStyle(color = friendOffline))
    builder.append("*")

    return builder.toAnnotatedString()
}