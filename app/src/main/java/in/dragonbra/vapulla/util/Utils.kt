package `in`.dragonbra.vapulla.util

import androidx.core.text.HtmlCompat
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.manager.AccountManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import timber.log.Timber

object Utils {

    object Constants {
        const val AVATAR_BASE_URL =
            "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/"

        const val ALL_ZEROS = "0000000000000000000000000000000000000000"

        const val MISSING_AVATAR_URL =
            "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

        const val PROFILE_URL = "https://steamcommunity.com/profiles/"

        const val BASE_STEAM_STORE_API_URL = "https://store.steampowered.com/api/"
        const val BASE_STEAM_STORE_URL = "https://store.steampowered.com/app/"
        const val EMOTICON_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
        const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    }

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val systemZone = ZoneId.systemDefault()

    // val dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

    fun String.decodeHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    /**
     * Gets the profile URL from a steam id.
     * Steam should redirect to a vanity URL if applied.
     */
    fun getProfileUrl(id: Long): String = "${Constants.PROFILE_URL}$id/"

    fun getAvatarURL(string: String?): String = string.orEmpty()
        .ifEmpty { null }
        ?.takeIf { str -> str.isNotEmpty() && !str.all { it == '0' } }
        ?.let { "${Constants.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
        ?: Constants.MISSING_AVATAR_URL

    fun Long.toDateString(): String {
        val instant = Instant.ofEpochSecond(this)
        val localDateTime = LocalDateTime.ofInstant(instant, systemZone)
        return localDateTime.format(dateFormatter)
    }

    fun Long.toTimeString(): String {
        val instant = Instant.ofEpochMilli(this)
        val localDateTime = LocalDateTime.ofInstant(instant, systemZone)
        return localDateTime.format(timeFormatter)
    }

    fun Long.toTimeAgo(): String {
        val diffSeconds = (System.currentTimeMillis() - this) / 1000
        return when {
            diffSeconds >= 31_536_000 -> "${diffSeconds / 31_536_000} years ago"
            diffSeconds >= 2_592_000 -> "${diffSeconds / 2_592_000} months ago"
            diffSeconds >= 604_800 -> "${diffSeconds / 604_800} weeks ago"
            diffSeconds >= 86_400 -> "${diffSeconds / 86_400} days ago"
            diffSeconds >= 3_600 -> "${diffSeconds / 3_600} hours ago"
            diffSeconds >= 60 -> "${diffSeconds / 60} minutes ago"
            else -> "Just now"
        }
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
                return message.take(result.end() - 1) + findEmotes(
                    message.substring(result.end() - 1),
                    emoteSet,
                )
            }
        } else {
            return message
        }
    }

    suspend fun getUniqueId(accountManager: AccountManager): Int =
        accountManager.uuid.first() ?: run {
            val uniqueID = abs(UUID.randomUUID().mostSignificantBits.toInt())
            accountManager.setUuid(uniqueID)
            uniqueID
        }

    fun printKeyValue(keyvalue: KeyValue, depth: Int) {
        if (keyvalue.children.isEmpty()) {
            Timber.tag("KeyValue")
                .d(" ".repeat(depth * 4) + " " + keyvalue.name + ": " + keyvalue.value)
        } else {
            Timber.tag("KeyValue").d(" ".repeat(depth * 4) + " " + keyvalue.name + ":")
            for (child in keyvalue.children) printKeyValue(child, depth + 1)
        }
    }
}
