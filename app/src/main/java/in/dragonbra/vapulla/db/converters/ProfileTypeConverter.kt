package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.vapulla.data.ProfileItems
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ProfileTypeConverter {
    @TypeConverter
    fun fromProfileItem(profileItem: ProfileItems): String {
        val serializable = SerializableProfileItem(
            communityItemId = profileItem.communityItemId,
            imageSmall = profileItem.imageSmall,
            imageLarge = profileItem.imageLarge,
            name = profileItem.name,
            itemTitle = profileItem.itemTitle,
            itemDescription = profileItem.itemDescription,
            appId = profileItem.appId,
            itemType = profileItem.itemType,
            itemClass = profileItem.itemClass,
            movieWebm = profileItem.movieWebm,
            movieMp4 = profileItem.movieMp4,
            movieWebmSmall = profileItem.movieWebmSmall,
            movieMp4Small = profileItem.movieMp4Small,
            equippedFlags = profileItem.equippedFlags,
            profileColors = profileItem.profileColors.map {
                SerializableProfileColor(it.styleName, it.color)
            },
            tiled = profileItem.tiled,
        )
        return Json.encodeToString(serializable)
    }

    @TypeConverter
    fun toProfileItem(value: String): ProfileItems {
        val serializable = Json.decodeFromString<SerializableProfileItem>(value)
        return ProfileItems(
            communityItemId = serializable.communityItemId,
            imageSmall = serializable.imageSmall,
            imageLarge = serializable.imageLarge,
            name = serializable.name,
            itemTitle = serializable.itemTitle,
            itemDescription = serializable.itemDescription,
            appId = serializable.appId,
            itemType = serializable.itemType,
            itemClass = serializable.itemClass,
            movieWebm = serializable.movieWebm,
            movieMp4 = serializable.movieMp4,
            movieWebmSmall = serializable.movieWebmSmall,
            movieMp4Small = serializable.movieMp4Small,
            equippedFlags = serializable.equippedFlags,
            profileColors = serializable.profileColors.map {
                ProfileItems.ProfileColor(it.styleName, it.color)
            }.toImmutableList(),
            tiled = serializable.tiled,
        )
    }

    @Serializable
    private data class SerializableProfileItem(
        val communityItemId: Long,
        val imageSmall: String,
        val imageLarge: String,
        val name: String,
        val itemTitle: String,
        val itemDescription: String,
        val appId: Int,
        val itemType: Int,
        val itemClass: Int,
        val movieWebm: String,
        val movieMp4: String,
        val movieWebmSmall: String,
        val movieMp4Small: String,
        val equippedFlags: Int,
        val profileColors: List<SerializableProfileColor>,
        val tiled: Boolean,
    )

    @Serializable
    private data class SerializableProfileColor(val styleName: String, val color: String)
}
