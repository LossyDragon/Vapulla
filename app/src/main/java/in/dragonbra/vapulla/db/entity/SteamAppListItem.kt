package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable

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
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$clientIconHash.ico"

    /**
     * 460x215 store header. Uses the asset from PICS when present and falls back to the
     * unversioned CDN path, which exists for almost every store app.
     */
    val headerImageUrl: String
        get() = headerImage[SteamApp.Language.english]
            ?.let { steamStoreAssetUrl(id, it) }
            ?: "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/header.jpg"
}
