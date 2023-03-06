@file:Suppress("MemberVisibilityCanBePrivate")

package `in`.dragonbra.vapulla.core

import android.os.Build
import java.util.regex.Pattern

/**
 * This class provides Constant related values
 */

object Constants {

    // Room Database
    const val DATABASE_NAME = "vapulla.db"

    // Steam Emoticons & Stickers
    val EMOTE_PATTERN: Pattern = Pattern.compile(":([a-zA-Z0-9]+):")
    val STICKER_PATTERN: Pattern = Pattern.compile("/sticker ([a-zA-Z0-9]+)")

    // Steam URLs
    const val BASE_STEAM_API_URL = "https://api.steampowered.com/"
    const val BASE_STEAM_STORE_URL = "https://store.steampowered.com/api/"
    const val EMOTE_URL = "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/"
    const val PROFILE_URL = "https://steamcommunity.com/profiles/"
    const val STEAM_CDN = "https://cdn.akamai.steamstatic.com"
    const val STICKER_URL = "https://steamcommunity-a.akamaihd.net/economy/sticker/"
    const val STORE_PAGE_URL = "https://store.steampowered.com/app/%d/"

    // Steam Avatar
    const val ALL_ZEROS = "0000000000000000000000000000000000000000"
    const val AVATAR_URL = "$STEAM_CDN/steamcommunity/public/images/avatars/"
    const val DEFAULT_AVATAR = "$AVATAR_URL/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"

    // Steam Games
    const val GAME_LOGO_URL = "$STEAM_CDN/steam/apps/%d/header.jpg"

    // Android Build Version
    val isAtLeastO
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val isAtLeastP
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val isAtLeastS
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isAtLeastT
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
