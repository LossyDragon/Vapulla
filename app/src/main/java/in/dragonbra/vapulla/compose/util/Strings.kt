package `in`.dragonbra.vapulla.compose.util

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.model.FriendListItem
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import timber.log.Timber

/**
 * This Class provides helpers that return a String of some kind
 */

/**
 * Get the last message time in ##:##AM/PM
 */
fun getLastMessageTime(friend: FriendListItem?): CharSequence? {
    if (friend == null || friend.lastMessage.isNullOrEmpty()) return ""
    return DateUtils.formatSameDayTime(
        friend.lastMessageTime?.times(1000) ?: 0,
        System.currentTimeMillis(),
        DateFormat.SHORT,
        DateFormat.SHORT
    )
}

/**
 * Gets the last log off time from a friend item and converts it to a relative time stamp.
 *
 * @param friend The friend item data class
 * @return The
 */
fun getLastSeenText(friend: FriendListItem?): CharSequence {
    return friend?.let {
        DateUtils.getRelativeTimeSpanString(
            it.lastLogOff,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
    } ?: "some time ago"
}

/**
 * Gets the avatar hash and constructs the full steam url at full size
 * @param avatar The avatar hash
 * @return The steam url of the hash, or default avatar if null
 */
fun getAvatarUrl(avatar: String?): String {
    return avatar?.takeIf { it.isNotEmpty() && it != Constants.ALL_ZEROS }
        ?.let { "${Constants.AVATAR_URL}${it.substring(0, 2)}/${it}_full.jpg" }
        ?: Constants.DEFAULT_AVATAR
}

/**
 * Converts a friends playtime into a approximate double of their playtime in hours.
 *
 * @param time The time a friend has played a game
 * @return A double representing how many hours were played, ie: 1.5 hrs
 */
fun formatPlayTime(time: Int): String {
    val df = DecimalFormat("#.#").apply {
        roundingMode = RoundingMode.CEILING
    }
    return df.format(time / 60.0)
}

/**
 * Gers the number of unread messages you have with a friend
 *
 * @param number The number of unread messages
 * @return The number of unread messages as a string, capped at 99
 */
fun getUnreadMessageCount(number: Int?): String {
    return number?.let { if (it > 99) "99+" else it.toString() } ?: "0"
}

/**
 * Gets the current status of a friend
 *
 * If the friend is null, we'll assume offline.
 *
 * @param friend The friend item data class
 * @return A formatted string containing their status
 */
fun Context.getStatusText(friend: FriendListItem?): String {
    if (friend == null) {
        return getString(R.string.statusOfflineLabel)
    }

    if (friend.isRequestRecipient()) {
        return getString(R.string.statusFriendRequest)
    }

    val isTypingFromLastMessage = friend.typingTs > (friend.lastMessageTime ?: 0)
    val isTyping = friend.typingTs > (System.currentTimeMillis() - 20000L)
    if (isTypingFromLastMessage && isTyping) {
        // Would be nice to have a typing indicator
        return getString(R.string.statusTyping)
    }

    if (friend.state == EPersonaState.Offline.code()) {
        return getString(R.string.statusOffline, getLastSeenText(friend))
    }

    if (friend.gameAppId != 0 || !friend.gameName.isNullOrEmpty()) {
        return getString(R.string.statusPlaying, friend.gameName ?: "")
    }

    val currentTime = System.currentTimeMillis()
    val resolution = DateUtils.MINUTE_IN_MILLIS
    val relativeDate =
        DateUtils.getRelativeTimeSpanString(friend.lastLogOff, currentTime, resolution)

    return when (EPersonaState.from(friend.state ?: 0)) {
        EPersonaState.Online -> getString(R.string.statusOnline)
        EPersonaState.Busy,
        EPersonaState.Away,
        EPersonaState.Snooze -> getString(R.string.statusAway)

        else -> getString(R.string.statusOffline, relativeDate)
    }
}

/**
 * Constructs a friend's name into a themed annotated string
 * Will display 'Loading..." if the friend is null
 * Name will be colorized according to their status
 * Nicknames will have a grey asterisk at the end.
 *
 * @param friend The Friend Item data class
 * @return A themed annotated string.
 */
fun getFriendName(friend: FriendListItem?): AnnotatedString {
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

/**
 * Gets the EResult of a unsuccessful login attempt
 *
 * @param eResult The EResult of the error
 * @param extendedResult The extended result of the error
 *
 * @return A string generalizing the error.
 */
fun Context.getErrorMessage(eResult: EResult, extendedResult: EResult? = null): String {
    Timber.w("getErrorMessage(): $extendedResult")
    return when (eResult) {
        EResult.NoConnection -> getString(R.string.errorMessageLostConnection)
        EResult.InvalidPassword -> getString(R.string.errorMessageInvalidPassword)
        EResult.TwoFactorCodeMismatch -> getString(R.string.errorMessageTwoFactorCodeMismatch)
        EResult.InvalidLoginAuthCode -> getString(R.string.errorMessageInvalidLoginAuthCode)
        else -> eResult.toString()
    }
}

/**
 * Goes through a message text from chat and transforms anything with an emote or sticker.
 */
fun findEmotes(message: String, emoteSet: Set<String>): String {
    val stickerMatcher = Constants.STICKER_PATTERN.matcher(message)

    Timber.d("Emote Set: $emoteSet")

    // Handling Stickers
    if (stickerMatcher.find()) {
        val emote = stickerMatcher.group(1)

        if (emoteSet.contains(emote!!)) {
            Timber.d("Matched Sticker: $message")
            return "[sticker type=\"$emote\" limit=\"0\"][/sticker]"
        }
    }

    // Handling Emotes
    val emoteMatcher = Constants.EMOTE_PATTERN.matcher(message)
    var resultMessage = message

    while (emoteMatcher.find()) {
        val emote = emoteMatcher.group(1)

        if (emoteSet.contains(emote!!)) {
            val builder = StringBuilder(resultMessage)
            builder.setCharAt(emoteMatcher.start(), '\u02D0')
            builder.setCharAt(emoteMatcher.end() - 1, '\u02D0')
            resultMessage = builder.toString()
        }
    }

    Timber.d("Matched Emotes: $resultMessage")
    return resultMessage
}
