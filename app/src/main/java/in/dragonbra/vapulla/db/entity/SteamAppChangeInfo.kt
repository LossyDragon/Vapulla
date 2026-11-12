package `in`.dragonbra.vapulla.db.entity

/**
 * Minimal projection of [SteamApp] used to decide whether incoming PICS data is newer than
 * what is stored, without reading (and JSON-decoding) the full row.
 */
data class SteamAppChangeInfo(val id: Int, val packageId: Int, val lastChangeNumber: Int)
