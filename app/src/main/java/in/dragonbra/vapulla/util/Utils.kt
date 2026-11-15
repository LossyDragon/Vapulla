package `in`.dragonbra.vapulla.util

import androidx.core.text.HtmlCompat
import `in`.dragonbra.vapulla.manager.AccountManager
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
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

        const val GROUP_AVATAR_BASE_URL = "https://avatars.steamstatic.com/"

        /** Avatar hash Steam reports for accounts without a custom avatar. */
        const val ALL_ZEROS = "0000000000000000000000000000000000000000"

        const val MISSING_AVATAR_URL =
            "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

        const val PROFILE_URL = "https://steamcommunity.com/profiles/"
        const val CUSTOM_URL = "https://steamcommunity.com/id/"

        /** Where an app's logo and icons are; the app ID and the image's file name follow. */
        const val APP_IMAGES_URL =
            "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/"
        const val EMOTICON_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
        const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"

        /** A video is opened by adding its ID; its thumbnail has `%s` where the ID goes. */
        const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
        const val YOUTUBE_THUMBNAIL_URL = "https://img.youtube.com/vi/%s/hqdefault.jpg"
    }

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE
    private const val SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR
    private const val SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY

    /** Months and years are rounded to 30 and 365 days, which is close enough for "ago". */
    private const val SECONDS_PER_MONTH = 30 * SECONDS_PER_DAY
    private const val SECONDS_PER_YEAR = 365 * SECONDS_PER_DAY

    /** Steam names and summaries may contain HTML entities. */
    fun String.decodeHtml(): String =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    /** Profile URL for a 64-bit Steam ID. Steam redirects to the vanity URL if one is set. */
    fun getProfileUrl(id: Long): String = "${Constants.PROFILE_URL}$id/"

    /**
     * [cents] as money in [currencyCode], e.g. "$9.99". Steam counts every currency in
     * hundredths, and names some that the device may not know; those are shown as a plain
     * number with the code.
     */
    fun formatMoney(cents: Long, currencyCode: String): String {
        val amount = cents / 100.0

        return runCatching {
            NumberFormat.getCurrencyInstance()
                .apply { currency = Currency.getInstance(currencyCode) }
                .format(amount)
        }.getOrElse { "%.2f %s".format(amount, currencyCode).trim() }
    }

    /** Full-size avatar URL for an avatar hash, or the default avatar when there is none. */
    fun getAvatarURL(hash: String?): String = hash
        ?.takeIf { it.isNotEmpty() && !it.all { c -> c == '0' } }
        ?.let { "${Constants.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
        ?: Constants.MISSING_AVATAR_URL

    /**
     * Avatar URL for a group chat's `chat_group_avatar_sha`, or null when the group has none.
     * Group avatars live on their own host and only come in the 256px size.
     */
    fun getGroupAvatarURL(hash: String): String? = hash
        .takeIf { it.isNotEmpty() && !it.all { c -> c == '0' } }
        ?.let { "${Constants.GROUP_AVATAR_BASE_URL}${it}_256.jpg" }

    // The time zone is asked for each time, so a change of it while the app runs is followed.

    /** Epoch seconds to "Jan 5, 2024". */
    fun Long.toDateString(): String =
        Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault()).format(dateFormatter)

    /** Epoch milliseconds to "3:07 PM". */
    fun Long.toTimeString(): String =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(timeFormatter)

    /** Minutes of playtime to "45 min", "12.5 h" or "1,204 h"; tenths are shown below 100 h. */
    fun Int.minutesToPlaytime(): String = when {
        this < 60 -> "$this min"
        this < 100 * 60 -> String.format(Locale.getDefault(), "%.1f h", this / 60f)
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

    /** Epoch milliseconds to "3 hours ago", counted back from [now]. */
    fun Long.toTimeAgo(now: Long = System.currentTimeMillis()): String {
        val seconds = (now - this) / 1000
        return when {
            seconds >= SECONDS_PER_YEAR -> (seconds / SECONDS_PER_YEAR).ago("year")
            seconds >= SECONDS_PER_MONTH -> (seconds / SECONDS_PER_MONTH).ago("month")
            seconds >= SECONDS_PER_WEEK -> (seconds / SECONDS_PER_WEEK).ago("week")
            seconds >= SECONDS_PER_DAY -> (seconds / SECONDS_PER_DAY).ago("day")
            seconds >= SECONDS_PER_HOUR -> (seconds / SECONDS_PER_HOUR).ago("hour")
            seconds >= SECONDS_PER_MINUTE -> (seconds / SECONDS_PER_MINUTE).ago("minute")
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
