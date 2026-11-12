package `in`.dragonbra.vapulla.util

import androidx.core.text.HtmlCompat
import `in`.dragonbra.vapulla.manager.AccountManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.abs
import kotlinx.coroutines.flow.first

/** Steam URLs plus small formatting helpers used across the UI. */
object Utils {

    object Constants {
        const val AVATAR_BASE_URL =
            "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/"

        /** Avatar hash Steam reports for accounts without a custom avatar. */
        const val ALL_ZEROS = "0000000000000000000000000000000000000000"

        const val MISSING_AVATAR_URL =
            "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

        const val PROFILE_URL = "https://steamcommunity.com/profiles/"
        const val BASE_STEAM_STORE_URL = "https://store.steampowered.com/app/"
        const val EMOTICON_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
        const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    }

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val systemZone = ZoneId.systemDefault()

    /** Steam names and summaries may contain HTML entities. */
    fun String.decodeHtml(): String =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    /** Profile URL for a 64-bit Steam ID. Steam redirects to the vanity URL if one is set. */
    fun getProfileUrl(id: Long): String = "${Constants.PROFILE_URL}$id/"

    fun String.toAvatarURL(): String = getAvatarURL(this)

    /** Full-size avatar URL for an avatar hash, or the default avatar when there is none. */
    fun getAvatarURL(hash: String?): String = hash
        ?.takeIf { it.isNotEmpty() && !it.all { c -> c == '0' } }
        ?.let { "${Constants.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
        ?: Constants.MISSING_AVATAR_URL

    /** Epoch seconds to "Jan 5, 2024". */
    fun Long.toDateString(): String =
        Instant.ofEpochSecond(this).atZone(systemZone).format(dateFormatter)

    /** Epoch milliseconds to "3:07 PM". */
    fun Long.toTimeString(): String =
        Instant.ofEpochMilli(this).atZone(systemZone).format(timeFormatter)

    /** Minutes of playtime to "45 min", "12.5 h" or "1,204 h". */
    fun Int.minutesToPlaytime(): String = when {
        this < 60 -> "$this min"
        this < 6_000 -> String.format(Locale.getDefault(), "%.1f h", this / 60f)
        else -> String.format(Locale.getDefault(), "%,d h", this / 60)
    }

    /** Bytes to "1.2 MB" style text. */
    fun Long.toFileSize(): String {
        if (this < 1024) return "$this B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = this / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    }

    /** Epoch milliseconds to "3 hours ago". */
    fun Long.toTimeAgo(): String {
        val diffSeconds = (System.currentTimeMillis() - this) / 1000
        return when {
            diffSeconds >= 31_536_000 -> (diffSeconds / 31_536_000).ago("year")
            diffSeconds >= 2_592_000 -> (diffSeconds / 2_592_000).ago("month")
            diffSeconds >= 604_800 -> (diffSeconds / 604_800).ago("week")
            diffSeconds >= 86_400 -> (diffSeconds / 86_400).ago("day")
            diffSeconds >= 3_600 -> (diffSeconds / 3_600).ago("hour")
            diffSeconds >= 60 -> (diffSeconds / 60).ago("minute")
            else -> "Just now"
        }
    }

    /**
     * Marks `:emote:` tokens that match an owned emoticon by replacing their colons with
     * `ː` (U+02D0), so the chat renderer can tell real emotes from literal colons.
     */
    fun findEmotes(message: String, emoteSet: Set<String>): String {
        val matcher = EMOTE_PATTERN.matcher(message)
        if (!matcher.find()) return message

        val result = matcher.toMatchResult()
        val emote = result.group(1)

        return if (emote in emoteSet) {
            val marked = StringBuilder(message).apply {
                setCharAt(result.start(), 'ː')
                setCharAt(result.end() - 1, 'ː')
            }
            findEmotes(marked.toString(), emoteSet)
        } else {
            // Not an emote we own: keep it and continue after it, re-using its closing colon
            // in case it opens the next token.
            message.take(result.end() - 1) +
                findEmotes(message.substring(result.end() - 1), emoteSet)
        }
    }

    /** Stable per-install login ID Steam uses to tell sessions apart; created on first use. */
    suspend fun getUniqueId(accountManager: AccountManager): Int =
        accountManager.uuid.first() ?: run {
            val uniqueID = abs(UUID.randomUUID().mostSignificantBits.toInt())
            accountManager.setUuid(uniqueID)
            uniqueID
        }
}

/** "1 hour ago" / "3 hours ago" for [Utils.toTimeAgo]. */
private fun Long.ago(unit: String): String = if (this == 1L) "1 $unit ago" else "$this ${unit}s ago"
