package `in`.dragonbra.vapulla.data

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ProfileItem(
    val communityItemId: Long = 0L,
    val imageSmall: String = "",
    val imageLarge: String = "",
    val name: String = "",
    val itemTitle: String = "",
    val itemDescription: String = "",
    val appId: Int = 0,
    val itemType: Int = 0,
    val itemClass: Int = 0,
    val movieWebm: String = "",
    val movieMp4: String = "",
    val movieWebmSmall: String = "",
    val movieMp4Small: String = "",
    val equippedFlags: Int = 0,
    val profileColors: ImmutableList<ProfileColor> = persistentListOf(),
    val tiled: Boolean = false,
) {
    @Immutable
    data class ProfileColor(
        val styleName: String = "",
        val color: String = "",
    )

    companion object {
        fun serialize(item: SteammessagesPlayerSteamclient.ProfileItem): ProfileItem {
            return ProfileItem(
                communityItemId = item.communityitemid,
                imageSmall = item.imageSmall,
                imageLarge = item.imageLarge,
                name = item.name,
                itemTitle = item.itemTitle,
                itemDescription = item.itemDescription,
                appId = item.appid,
                itemType = item.itemType,
                itemClass = item.itemClass,
                movieWebm = item.movieWebm,
                movieMp4 = item.movieMp4,
                movieWebmSmall = item.movieWebmSmall,
                movieMp4Small = item.movieMp4Small,
                equippedFlags = item.equippedFlags,
                profileColors = item.profileColorsList.map {
                    ProfileColor(
                        styleName = it.styleName,
                        color = it.color,
                    )
                }.toImmutableList(),
                tiled = item.tiled,
            )
        }
    }
}
