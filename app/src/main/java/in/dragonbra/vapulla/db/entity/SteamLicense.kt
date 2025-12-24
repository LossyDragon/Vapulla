package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.ELicenseType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import java.util.Date
import java.util.EnumSet

@Immutable
@Entity("steam_license")
data class SteamLicense(
    @PrimaryKey val packageID: Int,
    val lastChangeNumber: Int = 0,
    val timeCreated: Date = Date(0),
    val timeNextProcess: Date = Date(0),
    val minuteLimit: Int = 0,
    val minutesUsed: Int = 0,
    val paymentMethod: EPaymentMethod = EPaymentMethod.None,
    val licenseFlags: EnumSet<ELicenseFlags> = emptyEnumSet(),
    val purchaseCode: String = "",
    val licenseType: ELicenseType = ELicenseType.NoLicense,
    val territoryCode: Int = 0,
    val accessToken: Long = 0,
    val ownerAccountID: List<Int> = emptyList(),
    val masterPackageID: Int = 0,
    val appIds: List<Int> = emptyList(),
    val depotIds: List<Int> = emptyList(),
)
