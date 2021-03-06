package `in`.dragonbra.vapulla.data

import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.GameSchema
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SteamFriend::class, ChatMessage::class, GameSchema::class, Emoticon::class],
        version = 1, exportSchema = true)
abstract class VapullaDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "vapulla.db"
    }

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun gameSchemaDao(): GameSchemaDao

    abstract fun emoticonDao(): EmoticonDao
}
