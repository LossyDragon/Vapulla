package `in`.dragonbra.vapulla.data.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.GameID
import java.util.EnumSet
import android.util.Base64

class FriendTypeConverters {

    @TypeConverter
    fun fromEFriendRelationship(relationship: EFriendRelationship): Int {
        return relationship.code()
    }

    @TypeConverter
    fun toEFriendRelationship(code: Int): EFriendRelationship {
        return EFriendRelationship.from(code) ?: EFriendRelationship.None
    }

    @TypeConverter
    fun fromEPersonaState(state: EPersonaState): Int {
        return state.code()
    }

    @TypeConverter
    fun toEPersonaState(code: Int): EPersonaState {
        return EPersonaState.from(code) ?: EPersonaState.Offline
    }

    @TypeConverter
    fun fromEPersonaStateFlags(flags: EnumSet<EPersonaStateFlag>): Int {
        return EPersonaStateFlag.code(flags)
    }

    @TypeConverter
    fun toEPersonaStateFlags(code: Int): EnumSet<EPersonaStateFlag> {
        return EPersonaStateFlag.from(code)
    }

    @TypeConverter
    fun fromEClientPersonaStateFlag(flags: EnumSet<EClientPersonaStateFlag>): Int {
        return EClientPersonaStateFlag.code(flags)
    }

    @TypeConverter
    fun toEClientPersonaStateFlag(code: Int): EnumSet<EClientPersonaStateFlag> {
        return EClientPersonaStateFlag.from(code)
    }

    @TypeConverter
    fun fromGameID(gameId: GameID): Long {
        return gameId.toUInt64()
    }

    @TypeConverter
    fun toGameID(value: Long): GameID {
        return GameID(value)
    }

    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? {
        return bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
    }

    @TypeConverter
    fun toByteArray(value: String?): ByteArray? {
        return value?.let { Base64.decode(it, Base64.DEFAULT) }
    }
}