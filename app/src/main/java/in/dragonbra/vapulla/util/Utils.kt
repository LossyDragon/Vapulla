package `in`.dragonbra.vapulla.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import java.util.regex.Pattern

object Utils {

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")
    private val STICKER_PATTERN: Pattern = Pattern.compile("/sticker ([a-zA-Z0-9]+)")

    private const val ALL_ZEROS = "0000000000000000000000000000000000000000"
    private const val STEAM_AVATAR = "steamcommunity/public/images/avatars"
    private const val STEAM_CDN = "https://cdn.akamai.steamstatic.com"
    // private const val STEAM_MEDIA = "https://media.steampowered.com"

    const val EMOTE_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
    const val GAME_LOGO_URL = "$STEAM_CDN/steam/apps/%d/header.jpg"

    const val PROFILE_URL = "https://steamcommunity.com/profiles/"
    const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    const val STORE_PAGE_URL = "https://store.steampowered.com/app/%d/"
    private const val AVATAR_URL = "$STEAM_CDN/$STEAM_AVATAR/"
    private const val DEFAULT_AVATAR =
        "$AVATAR_URL/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

    val isAtLeastO
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val isAtLeastP
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val isAtLeastS
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isAtLeastT
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @Suppress("SameParameterValue")
    inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? {
        return when {
            isAtLeastT -> getParcelableArrayList(key, T::class.java)
            else -> {
                @Suppress("DEPRECATION")
                getParcelableArrayList(key)
            }
        }
    }

    fun getAvatarUrl(avatar: String?): String {
        if (avatar.isNullOrEmpty() || avatar == ALL_ZEROS) {
            return DEFAULT_AVATAR
        }

        return "$AVATAR_URL${avatar.substring(0, 2)}/${avatar}_full.jpg"
    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun findEmotes(message: String, emoteSet: Set<String>): String {
        val matcher = EMOTE_PATTERN.matcher(message)
        val matcher2 = STICKER_PATTERN.matcher(message)

        if (matcher2.find()) {
            val result = matcher2.toMatchResult()
            val emote = result.group(1)

            if (emoteSet.contains(emote)) {
                return "[sticker type=\"$emote\" limit=\"0\"][/sticker]"
            }
        }

        if (matcher.find()) {
            val result = matcher.toMatchResult()

            val emote = result.group(1)

            if (emoteSet.contains(emote)) {
                val builder = StringBuilder(message)
                builder.setCharAt(result.start(), '\u02D0')
                builder.setCharAt(result.end() - 1, '\u02D0')

                return findEmotes(builder.toString(), emoteSet)
            }

            return message.substring(0, result.end() - 1) +
                findEmotes(message.substring(result.end() - 1), emoteSet)
        }

        return message
    }
}
