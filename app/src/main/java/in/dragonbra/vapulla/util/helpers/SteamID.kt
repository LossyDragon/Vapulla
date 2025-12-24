package `in`.dragonbra.vapulla.util.helpers

import `in`.dragonbra.javasteam.types.SteamID

fun Long.toSteamID() = SteamID(this)

fun SteamID.toLong() = this.convertToUInt64()
