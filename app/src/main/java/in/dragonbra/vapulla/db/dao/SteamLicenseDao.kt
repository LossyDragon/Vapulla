package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.SteamLicense
import `in`.dragonbra.vapulla.db.entity.SteamLicenseInfo

@Dao
interface SteamLicenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(license: List<SteamLicense>)

    @Update
    suspend fun update(license: SteamLicense)

    @Query("SELECT * FROM steam_license")
    suspend fun getAllLicenses(): List<SteamLicense>

    @Query("UPDATE steam_license SET appIds = :appIds WHERE packageId = :packageId")
    suspend fun updateApps(packageId: Int, appIds: List<Int>)

    @Query("UPDATE steam_license SET depotIds = :depotIds WHERE packageId = :packageId")
    suspend fun updateDepots(packageId: Int, depotIds: List<Int>)

    @Query("SELECT * FROM steam_license WHERE packageId NOT IN (:packageIds)")
    suspend fun findStaleLicences(packageIds: List<Int>): List<SteamLicense>

    @Query("SELECT * FROM steam_license WHERE packageId = :packageId")
    suspend fun findLicense(packageId: Int): SteamLicense?

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

    /** The raw comma-separated depot list of every license, for working out what is owned. */
    @Query("SELECT depotIds FROM steam_license WHERE depotIds != ''")
    suspend fun getAllDepotIds(): List<String>

    @Query("DELETE FROM steam_license WHERE packageId IN (:packageIds)")
    suspend fun deleteStaleLicenses(packageIds: List<Int>)
}
