package `in`.dragonbra.vapulla.util.helpers

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID

/** The [SteamID] for a 64-bit Steam ID, the form the app stores and passes around. */
fun Long.toSteamID() = SteamID(this)

/** The 64-bit Steam ID. It is unsigned, so show it with [toSteamIdString]. */
fun SteamID.toLong() = this.convertToUInt64()

/**
 * The Steam ID of a person's account ID, the low half of a Steam ID, which is all that many
 * Steam messages carry.
 */
fun Int.accountIdToSteamID() = SteamID(toUInt().toLong(), EUniverse.Public, EAccountType.Individual)

/** A 64-bit Steam ID, or any other unsigned 64-bit ID Steam uses, as its decimal digits. */
fun Long.toSteamIdString(): String = toULong().toString()
