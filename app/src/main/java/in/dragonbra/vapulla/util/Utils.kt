package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getColor
import com.bumptech.glide.request.RequestOptions
import java.util.regex.Pattern

object Utils {

    val avatarOptions = RequestOptions().transform(CircleTransform())

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")
    private val STICKER_PATTERN: Pattern = Pattern.compile("/sticker ([a-zA-Z0-9]+)")

    private const val ALL_ZEROS = "0000000000000000000000000000000000000000"
    private const val STEAM_AVATAR = "steamcommunity/public/images/avatars"
    private const val STEAM_CDN = "https://cdn.akamai.steamstatic.com"
    private const val STEAM_MEDIA = "http://media.steampowered.com"

    const val EMOTE_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
    const val GAME_LOGO_URL = "$STEAM_MEDIA/steamcommunity/public/images/apps/%d/%s.jpg"
    const val PROFILE_URL = "https://steamcommunity.com/profiles/"
    const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    const val STORE_PAGE_URL = "http://store.steampowered.com/app/%d/"
    private const val AVATAR_URL = "$STEAM_CDN/$STEAM_AVATAR/"
    private const val DEFAULT_AVATAR =
        "$AVATAR_URL/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

    val isGreaterThanM
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val isLessThanN
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    val isAtLeastN
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val isGreaterThanO
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val isGreaterThanP
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun getAvatarUrl(avatar: String?): String {
        if (avatar.isNullOrEmpty() || avatar == ALL_ZEROS) {
            return DEFAULT_AVATAR
        }

        return "$AVATAR_URL${avatar.substring(0, 2)}/${avatar}_full.jpg"
    }

    fun getStatusColor(
        context: Context,
        state: EPersonaState?,
        gameAppId: Int,
        gameName: String?
    ): Int {
        if (gameAppId != 0 || !gameName.isNullOrEmpty()) {
            return getColor(context, R.color.statusInGame)
        }

        val color = when (state) {
            EPersonaState.Online -> R.color.statusOnline
            EPersonaState.Busy -> R.color.statusBusy
            EPersonaState.Away,
            EPersonaState.Snooze -> R.color.statusAway
            EPersonaState.LookingToTrade,
            EPersonaState.LookingToPlay -> R.color.statusLookingTo
            else -> R.color.statusOffline
        }

        return getColor(context, color)
    }

    fun getStatusText(
        context: Context,
        state: EPersonaState?,
        gameAppId: Int,
        gameName: String?,
        lastLogOff: Long
    ): String {
        if (gameAppId != 0 || !gameName.isNullOrEmpty()) {
            return context.getString(R.string.statusPlaying, gameName ?: "")
        }

        return when (state) {
            EPersonaState.Online -> context.getString(R.string.statusOnline)
            EPersonaState.Busy -> context.getString(R.string.statusBusy)
            EPersonaState.Away -> context.getString(R.string.statusAway)
            EPersonaState.Snooze -> context.getString(R.string.statusSnooze)
            EPersonaState.LookingToTrade -> context.getString(R.string.statusLookingTrade)
            EPersonaState.LookingToPlay -> context.getString(R.string.statusLookingPlay)
            else -> context.getString(
                R.string.statusOffline,
                DateUtils.getRelativeTimeSpanString(
                    lastLogOff,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            )
        }
    }

    fun hideKeyboardFrom(context: Context, view: View) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
