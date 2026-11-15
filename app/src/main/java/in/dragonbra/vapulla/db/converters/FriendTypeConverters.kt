package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.GameID
import java.util.EnumSet

/**
 * JavaSteam's friend enums and [GameID], stored as Steam's own numbers. A number this
 * version of JavaSteam does not know reads as "none" or offline.
 */
class FriendTypeConverters {

    @TypeConverter
    fun fromEFriendRelationship(relationship: EFriendRelationship): Int = relationship.code()

    @TypeConverter
    fun toEFriendRelationship(code: Int): EFriendRelationship =
        EFriendRelationship.from(code) ?: EFriendRelationship.None

    @TypeConverter
    fun fromEPersonaState(state: EPersonaState): Int = state.code()

    @TypeConverter
    fun toEPersonaState(code: Int): EPersonaState =
        EPersonaState.from(code) ?: EPersonaState.Offline

    @TypeConverter
    fun fromEPersonaStateFlags(flags: EnumSet<EPersonaStateFlag>): Int =
        EPersonaStateFlag.code(flags)

    @TypeConverter
    fun toEPersonaStateFlags(code: Int): EnumSet<EPersonaStateFlag> = EPersonaStateFlag.from(code)

    @TypeConverter
    fun fromEClientPersonaStateFlag(flags: EnumSet<EClientPersonaStateFlag>): Int =
        EClientPersonaStateFlag.code(flags)

    @TypeConverter
    fun toEClientPersonaStateFlag(code: Int): EnumSet<EClientPersonaStateFlag> =
        EClientPersonaStateFlag.from(code)

    @TypeConverter
    fun fromGameID(gameId: GameID): Long = gameId.toUInt64()

    @TypeConverter
    fun toGameID(value: Long): GameID = GameID(value)
}
