package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.db.entity.SteamLicense
import `in`.dragonbra.vapulla.db.entity.SteamLicenseInfo

/** The packages (licenses) of the account, and the apps and depots each one grants. */
@Dao
interface SteamLicenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(license: List<SteamLicense>)

    @Query("SELECT * FROM steam_license")
    suspend fun getAllLicenses(): List<SteamLicense>

    @Query("UPDATE steam_license SET appIds = :appIds WHERE packageID = :packageId")
    suspend fun updateApps(packageId: Int, appIds: List<Int>)

    @Query("UPDATE steam_license SET depotIds = :depotIds WHERE packageID = :packageId")
    suspend fun updateDepots(packageId: Int, depotIds: List<Int>)

    /**
     * Batched lookup of the few columns needed when stamping ownership onto PICS apps and when
     * checking for package changes. Callers should chunk [packageIds] to stay well under
     * SQLite's bound-parameter limit.
     */
    @Query(
        "SELECT packageID, lastChangeNumber, ownerAccountID, licenseFlags " +
            "FROM steam_license WHERE packageID IN (:packageIds)",
    )
    suspend fun findLicenseInfo(packageIds: List<Int>): List<SteamLicenseInfo>

    /** The depot list of every license that has one, as stored; see [ownedDepotIds]. */
    @Query("SELECT depotIds FROM steam_license WHERE depotIds != ''")
    suspend fun getAllDepotIds(): List<String>

    /** Every depot that some license of the account grants. */
    suspend fun ownedDepotIds(): Set<Int> = getAllDepotIds()
        .flatMapTo(HashSet()) { ids -> ids.split(',').mapNotNull(String::toIntOrNull) }

    @Query("DELETE FROM steam_license WHERE packageID IN (:packageIds)")
    suspend fun deleteStaleLicenses(packageIds: List<Int>)

    @Query("DELETE FROM steam_license")
    suspend fun deleteAll()
}
