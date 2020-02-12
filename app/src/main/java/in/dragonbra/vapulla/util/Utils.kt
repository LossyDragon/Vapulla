package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.RequestOptions
import java.util.regex.Pattern

object Utils {

    val avatarOptions = RequestOptions().transform(CircleTransform())

    private val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")
    private val STICKER_PATTERN: Pattern = Pattern.compile("/sticker ([a-zA-Z0-9]+)")

    private const val ALL_ZEROS = "0000000000000000000000000000000000000000"

    const val STORE_PAGE_URL = "http://store.steampowered.com/app/%d/"
    const val EMOTE_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
    const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    const val PROFILE_URL = "https://steamcommunity.com/profiles/"
    const val GAME_LOGO_URL = "http://media.steampowered.com/steamcommunity/public/" +
            "images/apps/%d/%s.jpg"
    private const val DEFAULT_AVATAR = "http://cdn.akamai.steamstatic.com/steamcommunity/public/" +
            "images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
    private const val AVATAR_URL = "http://cdn.akamai.steamstatic.com/steamcommunity/public/" +
            "images/avatars/"

    fun isLessThanN() = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    fun isAtLeastN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isGreaterThanO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isGreaterThanP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun getAvatarUrl(avatar: String?) =
            if (avatar == null || Strings.isNullOrEmpty(avatar) || avatar == ALL_ZEROS) {
                DEFAULT_AVATAR
            } else {
                "$AVATAR_URL${avatar.substring(0, 2)}/${avatar}_full.jpg"
            }

    fun getStatusColor(context: Context,
                       state: EPersonaState?,
                       gameAppId: Int, gameName: String?
    ) =
            if (state == EPersonaState.Offline ||
                    gameAppId == 0 && Strings.isNullOrEmpty(gameName)) {
                when (state) {
                    EPersonaState.Online ->
                        ContextCompat.getColor(context, R.color.statusOnline)
                    EPersonaState.Busy ->
                        ContextCompat.getColor(context, R.color.statusBusy)
                    EPersonaState.Away, EPersonaState.Snooze ->
                        ContextCompat.getColor(context, R.color.statusAway)
                    EPersonaState.LookingToTrade,
                    EPersonaState.LookingToPlay ->
                        ContextCompat.getColor(context, R.color.statusLookingTo)
                    else -> ContextCompat.getColor(context, R.color.statusOffline)
                }
            } else {
                ContextCompat.getColor(context, R.color.statusInGame)
            }

    fun getStatusText(context: Context,
                      state: EPersonaState?,
                      gameAppId: Int,
                      gameName: String?,
                      lastLogOff: Long
    ): String =
            if (state == EPersonaState.Offline ||
                    gameAppId == 0 && Strings.isNullOrEmpty(gameName)) {
                when (state) {
                    EPersonaState.Online -> context.getString(R.string.statusOnline)
                    EPersonaState.Busy -> context.getString(R.string.statusBusy)
                    EPersonaState.Away -> context.getString(R.string.statusAway)
                    EPersonaState.Snooze -> context.getString(R.string.statusSnooze)
                    EPersonaState.LookingToTrade -> context.getString(R.string.statusLookingTrade)
                    EPersonaState.LookingToPlay -> context.getString(R.string.statusLookingPlay)
                    else -> context.getString(R.string.statusOffline,
                            DateUtils.getRelativeTimeSpanString(
                                    lastLogOff,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS)
                    )
                }
            } else {
                context.getString(R.string.statusPlaying, gameName ?: "")
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

            return if (emoteSet.contains(emote)) {
                val builder = StringBuilder(message)
                builder.setCharAt(result.start(), '\u02D0')
                builder.setCharAt(result.end() - 1, '\u02D0')

                findEmotes(builder.toString(), emoteSet)
            } else {
                message.substring(0, result.end() - 1) +
                        findEmotes(message.substring(result.end() - 1), emoteSet)
            }
        } else {
            return message
        }
    }
}
