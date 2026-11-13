package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.GameID
import `in`.dragonbra.javasteam.types.GlobalID
import `in`.dragonbra.javasteam.types.SteamID
import java.util.Date

/** One way of reading an ID: what it would be, and the parts it breaks into. */
data class DecodedId(val title: String, val note: String, val rows: List<Pair<String, String>>)

/**
 * Breaks Steam's packed 64-bit IDs into their parts with JavaSteam's [SteamID], [GameID] and
 * [GlobalID]. A bare number does not say which kind it is, so every reading that makes sense
 * is returned and the caller shows them all.
 */
object IdDecoder {

    private val STEAM2 = Regex("""STEAM_\d:[01]:\d+""", RegexOption.IGNORE_CASE)
    private val STEAM3 = Regex("""\[[A-Za-z]:\d:\d+(:\d+)?]""")

    /** 2^32: below it a number can only be an account ID or an app ID, not a packed ID. */
    private const val ACCOUNT_ID_LIMIT = 1L shl 32

    /** Start of [GlobalID]'s clock (1 January 2005); an ID with no time in it decodes to this. */
    private const val GLOBAL_ID_EPOCH = 1_104_537_600_000L

    fun decode(input: String): List<DecodedId> {
        val text = input.trim()
        if (text.isEmpty()) return emptyList()

        if (STEAM2.matches(text)) {
            val id = SteamID(text.uppercase(), EUniverse.Public)
            return listOf(steamId(id, "From the Steam2 text form."))
        }

        if (STEAM3.matches(text)) {
            val id = SteamID()
            return if (id.setFromSteam3String(text)) {
                listOf(steamId(id, "From the Steam3 text form."))
            } else {
                emptyList()
            }
        }

        // Steam IDs are unsigned, so the largest ones do not fit a signed long as text.
        val number = text.toLongOrNull() ?: text.toULongOrNull()?.toLong() ?: return emptyList()

        if (number in 1 until ACCOUNT_ID_LIMIT) {
            val asAccount = SteamID(number, EUniverse.Public, EAccountType.Individual)
            return listOf(
                steamId(asAccount, "If this is an account ID, the low 32 bits of a Steam ID."),
                gameId(GameID(number), "If this is an app ID."),
            )
        }

        return buildList {
            val steam = SteamID(number)
            if (steam.isValid) {
                add(steamId(steam, "If this is a Steam ID: a user, group, chat, lobby or server."))
            }
            add(gameId(GameID(number), "If this is a game ID: an app, mod or non-Steam shortcut."))
            add(globalId(GlobalID(number)))
        }
    }

    private fun steamId(id: SteamID, note: String): DecodedId {
        val rows = mutableListOf(
            "64-bit" to id.convertToUInt64().toULong().toString(),
            "Steam3" to id.render(true),
        )
        if (id.isIndividualAccount) {
            rows += "Steam2" to id.render(false)
        }
        rows += listOf(
            "Universe" to id.accountUniverse.name,
            "Type" to (id.accountType?.name ?: "Unknown"),
            "Instance" to id.accountInstance.toString(),
            "Account ID" to id.accountID.toString(),
            "Valid" to if (id.isValid) "Yes" else "No",
        )
        if (id.isIndividualAccount) {
            rows += "Profile" to Utils.getProfileUrl(id.convertToUInt64())
        }

        return DecodedId("Steam ID", note, rows)
    }

    private fun gameId(id: GameID, note: String) = DecodedId(
        title = "Game ID",
        note = note,
        rows = listOf(
            "App ID" to id.appID.toString(),
            "Type" to (id.appType?.name ?: "Unknown"),
            "Mod ID" to id.modID.toString(),
        ),
    )

    private fun globalId(id: GlobalID): DecodedId {
        val started = id.startTime.time
        return DecodedId(
            title = "Global ID",
            note = "If this is a job, match, UGC or manifest ID made by a Steam server.",
            rows = listOf(
                "Box" to id.boxID.toString(),
                "Process" to id.processID.toString(),
                "Server started" to
                    if (started > GLOBAL_ID_EPOCH) Date(started).toString() else "Not set",
                "Sequence" to id.sequentialCount.toString(),
            ),
        )
    }
}
