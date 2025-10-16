package `in`.dragonbra.vapulla.data.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import java.util.EnumSet

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
}