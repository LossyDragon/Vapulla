package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import `in`.dragonbra.vapulla.util.Utils

/**
 * Lightweight projection of [SteamApp] for rendering lists.
 *
 * Selecting only these columns avoids deserializing the large JSON columns of [SteamApp]
 * (depots, branches, library assets, config, ufs) on every page load and on every table
 * invalidation, which happens continuously while PICS data is being ingested.
 */
@Immutable
data class SteamAppListItem(
    val id: Int,
    val name: String = "",
    val type: SteamApp.AppType = SteamApp.AppType.invalid,
    val developer: String = "",
    val publisher: String = "",
    val releaseDate: Long = 0L,
    val metacriticScore: Byte = 0,
    val clientIconHash: String = "",
    val ownerAccountId: List<Int> = emptyList(),
    val headerImage: Map<SteamApp.Language, String> = emptyMap(),
    val playtimeForever: Int = 0,
    val lastPlayed: Long = 0L,
) {
    val clientIconUrl: String
        get() = "${Utils.Constants.APP_IMAGES_URL}$id/$clientIconHash.ico"

    /** 460x215 store header; see [steamHeaderImageUrl]. */
    val headerImageUrl: String
        get() = steamHeaderImageUrl(id, headerImage)
}
