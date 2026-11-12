package `in`.dragonbra.vapulla.db.entity

import `in`.dragonbra.javasteam.enums.ELicenseFlags
import java.util.EnumSet

/**
 * Minimal projection of [SteamLicense] with just the fields needed when stamping ownership
 * onto incoming PICS apps and when checking for package changes.
 */
data class SteamLicenseInfo(
    val packageID: Int,
    val lastChangeNumber: Int,
    val ownerAccountID: List<Int>,
    val licenseFlags: EnumSet<ELicenseFlags>,
)
