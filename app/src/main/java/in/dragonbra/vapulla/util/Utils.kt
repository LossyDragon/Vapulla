package `in`.dragonbra.vapulla.util

import android.content.Context
import android.util.DisplayMetrics
import `in`.dragonbra.vapulla.manager.AccountManager
import java.util.UUID
import java.util.regex.Pattern

object Utils {

    object Constants {
        const val AVATAR_BASE_URL =
            "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/"
        const val MISSING_AVATAR_URL =
            "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
        const val PROFILE_URL = "https://steamcommunity.com/profiles/"
    }

    val EMOTE_PATTERN = Pattern.compile(":([a-zA-Z0-9]+):")

    fun getAvatarURL(string: String?): String =
        string.orEmpty()
            .ifEmpty { null }
            ?.takeIf { str -> str.isNotEmpty() && !str.all { it == '0' } }
            ?.let { "${Constants.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
            ?: Constants.MISSING_AVATAR_URL

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun findEmotes(message: String, emoteSet: Set<String>): String {
        val matcher = EMOTE_PATTERN.matcher(message)

        if (matcher.find()) {
            val result = matcher.toMatchResult()

            val emote = result.group(1)

            if (emoteSet.contains(emote)) {
                val builder = StringBuilder(message)
                builder.setCharAt(result.start(), '\u02D0')
                builder.setCharAt(result.end() - 1, '\u02D0')

                return findEmotes(builder.toString(), emoteSet)
            } else {
                return message.substring(
                    0,
                    result.end() - 1
                ) + findEmotes(message.substring(result.end() - 1), emoteSet)
            }
        } else {
            return message
        }
    }

    fun getUniqueId(accountManager: AccountManager): Int {
        if (accountManager.uuid == 0) {
            val uniqueID = UUID.randomUUID()
            accountManager.uuid = uniqueID.hashCode()
        }

        return accountManager.uuid
    }
}
