package `in`.dragonbra.vapulla.service.sync

import androidx.room.withTransaction
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamLicense
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.service.api.LibraryApi
import `in`.dragonbra.vapulla.util.generateSteamApp
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import `in`.dragonbra.vapulla.util.timeChunked
import java.io.Closeable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Mirrors the account's library into the database: the license list, then the PICS package
 * info of each license, then the PICS app info of each app in those packages, kept current by
 * polling the PICS change list. Also the lookup point for any single app's info.
 *
 * @param scope outlives a single sign-in; used for work started by a callback.
 * @param onAppsNamed told the names (by app id) of apps whose info was just stored.
 */
class LicenseSync(
    private val scope: CoroutineScope,
    private val db: VapullaDatabase,
    private val account: AccountManager,
    private val steamApps: SteamApps,
    private val library: LibraryApi,
    private val localSteamId: () -> SteamID?,
    private val onAppsNamed: suspend (Map<Int, String>) -> Unit,
) : SessionComponent {

    /** App ids waiting for a PICS app info request; drained in batches while signed in. */
    private val picsRequestChannel = Channel<Int>(Channel.UNLIMITED)

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribe(onPicsChanges),
        callbacks.subscribe(onPicsProduct),
        callbacks.subscribe(onLicenseList),
    )

    override fun start(session: CoroutineScope) {
        session.continuousPicsChanges()
        session.continuousPicsChecker()
        session.syncPlaytime()
    }

    /** Queues a PICS app info request for [appId]; the reply is stored like any other. */
    fun requestAppInfo(appId: Int) {
        // The channel is unlimited, so this never fails for lack of room.
        picsRequestChannel.trySend(appId)
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer {
        scope.launch {
            if (account.lastChangeNumber.first() == it.currentChangeNumber) {
                Timber.w("Change number was the same as last change number, skipping")
                return@launch
            }

            Timber.d("onPicsChanges()")

            account.setLastChangeNumber(it.currentChangeNumber)

            // Only queue PICS requests for apps existing in the db that have changed.
            // Lookups are batched and select only the columns needed for the comparison.
            it.appChanges.values
                .chunked(SQL_IN_CHUNK_SIZE)
                .flatMap { chunk ->
                    val known = db.steamAppDao()
                        .findChangeInfo(chunk.map { change -> change.id })
                        .associateBy { info -> info.id }

                    chunk.filter { change ->
                        val app = known[change.id] ?: return@filter false
                        change.changeNumber != app.lastChangeNumber
                    }
                }
                .map { value -> value.id }
                .also { value -> Timber.d("onPicsChanges: Queueing ${value.size} app requests") }
                .forEach { value -> requestAppInfo(value) }

            // Only queue PICS requests for pkgs existing in the db that have changed.
            val pkgsWithChanges = it.packageChanges.values
                .chunked(SQL_IN_CHUNK_SIZE)
                .flatMap { chunk ->
                    val known = db.steamLicenseDao()
                        .findLicenseInfo(chunk.map { change -> change.id })
                        .associateBy { info -> info.packageID }

                    chunk.filter { change ->
                        val pkg = known[change.id] ?: return@filter false
                        change.changeNumber != pkg.lastChangeNumber
                    }
                }

            if (pkgsWithChanges.isEmpty()) {
                return@launch
            }

            val pkgsForAccessTokens = pkgsWithChanges
                .filter { value -> value.isNeedsToken }
                .map { value -> value.id }

            val accessTokens = if (pkgsForAccessTokens.isNotEmpty()) {
                steamApps
                    .picsGetAccessTokens(appIds = emptyList(), packageIds = pkgsForAccessTokens)
                    .await()
                    .packageTokens
            } else {
                emptyMap()
            }

            val picsRequest = pkgsWithChanges.map { value ->
                PICSRequest(value.id, accessTokens[value.id] ?: 0)
            }

            Timber.d("onPicsChanges: Queueing ${picsRequest.size} package requests")
            steamApps.picsGetProductInfo(apps = emptyList(), packages = picsRequest)
        }
    }

    private val onPicsProduct: Consumer<PICSProductInfoCallback> = Consumer { callback ->
        Timber.d("onPicsProduct()")

        scope.launch {
            val appDao = db.steamAppDao()
            val licenseDao = db.steamLicenseDao()

            if (callback.packages.isNotEmpty()) {
                Timber.i("onPicsProduct: Received PICS of ${callback.packages.size} package(s)")

                val appIdsToRequest = mutableListOf<Int>()

                // One transaction for the whole batch: Room's invalidation tracker then notifies
                // observers (e.g. the paged games list) once, instead of once per statement.
                db.withTransaction {
                    callback.packages.values.forEach { pkg ->
                        val appIds = pkg.keyValues["appids"].children.map { it.asInteger() }
                        licenseDao.updateApps(pkg.id, appIds)

                        val depotIds = pkg.keyValues["depotids"].children.map { it.asInteger() }
                        licenseDao.updateDepots(pkg.id, depotIds)

                        // Insert a stub row for apps we don't know yet, then point every app
                        // in this package at it. This replaces a per-app SELECT * + full-row
                        // UPDATE that re-serialized every JSON column just to set packageId.
                        appDao.insertIfAbsent(appIds.map { SteamApp(id = it, packageId = pkg.id) })
                        appDao.updatePackageId(pkg.id, appIds)

                        appIdsToRequest += appIds
                    }
                }

                appIdsToRequest.forEach { requestAppInfo(it) }
            }

            if (callback.apps.isNotEmpty()) {
                Timber.i("onPicsProduct: Received PICS of ${callback.apps.size} apps(s)")

                // Batch the lookups and select only the columns needed to decide what changed,
                // instead of a SELECT * (with JSON decoding of every column) per app.
                val changeInfoById = callback.apps.keys
                    .chunked(SQL_IN_CHUNK_SIZE)
                    .flatMap { ids -> appDao.findChangeInfo(ids) }
                    .associateBy { info -> info.id }

                val licenseByPackageId = changeInfoById.values
                    .map { info -> info.packageId }
                    .filter { packageId -> packageId != Int.MAX_VALUE }
                    .distinct()
                    .chunked(SQL_IN_CHUNK_SIZE)
                    .flatMap { ids -> licenseDao.findLicenseInfo(ids) }
                    .associateBy { license -> license.packageID }

                val apps = callback.apps.values.mapNotNull { app ->
                    val changeInfo = changeInfoById[app.id]
                    if (app.changeNumber == changeInfo?.lastChangeNumber) {
                        return@mapNotNull null
                    }

                    val packageId = changeInfo?.packageId ?: Int.MAX_VALUE
                    val license = licenseByPackageId[packageId]

                    app.keyValues.generateSteamApp().copy(
                        packageId = packageId,
                        ownerAccountId = license?.ownerAccountID ?: emptyList(),
                        receivedPICS = true,
                        lastChangeNumber = app.changeNumber,
                        licenseFlags = license?.licenseFlags ?: emptyEnumSet(),
                    )
                }

                if (apps.isNotEmpty()) {
                    Timber.d("Inserting ${apps.size} PICS apps to database")
                    appDao.insert(apps)

                    // Friends already playing one of these apps were stored without a game
                    // name. Have it filled in now rather than at the next periodic check.
                    onAppsNamed(
                        apps
                            .filter { app -> app.name.isNotEmpty() }
                            .associate { app -> app.id to app.name },
                    )
                }
            }
        }
    }

    private val onLicenseList: Consumer<LicenseListCallback> = Consumer {
        Timber.d("onLicenseList()")

        val result = it.result

        if (result != EResult.OK) {
            Timber.w("Failed to get license list: $result")
            return@Consumer
        }

        scope.launch {
            delay(5.seconds)

            val incomingLicenses = it.licenseList
            val existingLicenses = db.steamLicenseDao().getAllLicenses()
            val myAccountId = localSteamId()?.accountID?.toInt()

            // Create lookup maps for efficient comparison
            val existingLicenseMap =
                existingLicenses.associateBy { license -> license.packageID }
            val incomingPackageIds =
                incomingLicenses.map { license -> license.packageID }.toSet()

            // Find licenses that need to be added or updated. A package can be licensed to
            // several accounts (family sharing); prefer our own copy for the details.
            val licensesToProcess = it.licenseList
                .groupBy { license -> license.packageID }
                .mapNotNull { entry ->
                    val preferredAccount = entry.value
                        .firstOrNull { value -> value.ownerAccountID == myAccountId }
                        ?: entry.value.first()

                    val existingLicense = existingLicenseMap[entry.key]

                    // Only process if:
                    // 1. License doesn't exist in DB, OR
                    // 2. License has been updated (different lastChangeNumber)
                    if (existingLicense == null ||
                        existingLicense.lastChangeNumber != preferredAccount.lastChangeNumber
                    ) {

                        SteamLicense(
                            packageID = entry.key,
                            lastChangeNumber = preferredAccount.lastChangeNumber,
                            timeCreated = preferredAccount.timeCreated,
                            timeNextProcess = preferredAccount.timeNextProcess,
                            minuteLimit = preferredAccount.minuteLimit,
                            minutesUsed = preferredAccount.minutesUsed,
                            paymentMethod = preferredAccount.paymentMethod,
                            licenseFlags = entry.value.fold(emptyEnumSet()) { flags, value ->
                                flags.apply { addAll(value.licenseFlags) }
                            },
                            purchaseCode = preferredAccount.purchaseCode,
                            licenseType = preferredAccount.licenseType,
                            territoryCode = preferredAccount.territoryCode,
                            accessToken = preferredAccount.accessToken,
                            ownerAccountID = entry.value.map { value -> value.ownerAccountID },
                            masterPackageID = preferredAccount.masterPackageID,
                            // Preserve existing app/depot data if updating
                            appIds = existingLicense?.appIds ?: emptyList(),
                            depotIds = existingLicense?.depotIds ?: emptyList(),
                        )
                    } else {
                        null // Skip unchanged licenses
                    }
                }

            if (licensesToProcess.isNotEmpty()) {
                Timber.i("Adding/updating ${licensesToProcess.size} licenses")
                db.steamLicenseDao().insert(licensesToProcess)
            }

            // Find licenses that were removed (exist in DB but not in incoming list)
            val licensesToRemove = existingLicenses.filter { license ->
                license.packageID !in incomingPackageIds
            }

            if (licensesToRemove.isNotEmpty()) {
                Timber.i("Removing ${licensesToRemove.size} stale licenses")

                // Clean up associated SteamApps
                licensesToRemove.forEach { license ->
                    // Remove apps that were only associated with this package
                    license.appIds.forEach { appId ->
                        val app = db.steamAppDao().findApp(appId)
                        if (app != null && app.packageId == license.packageID) {
                            // Reset to stub state or remove entirely
                            db.steamAppDao().update(
                                app.copy(
                                    packageId = Int.MAX_VALUE,
                                    ownerAccountId = emptyList(),
                                    licenseFlags = emptyEnumSet(),
                                ),
                            )
                        }
                    }
                }

                val packageIds = licensesToRemove.map { it.packageID }
                db.steamLicenseDao().deleteStaleLicenses(packageIds)
            }

            // Only request PICS for NEW or UPDATED licenses
            if (licensesToProcess.isNotEmpty()) {
                val picsRequests = licensesToProcess.map { license ->
                    PICSRequest(license.packageID, license.accessToken)
                }

                Timber.d("Requesting PICS for ${picsRequests.size} licenses")
                steamApps.picsGetProductInfo(apps = emptyList(), packages = picsRequests)
            }

            // Resume an interrupted ingest. Package info creates stub app rows and queues their
            // app info; if the process died before the replies arrived, nothing else would
            // ever ask again because the licenses themselves are unchanged.
            val awaitingPics = db.steamAppDao().findAppsAwaitingPics()
            if (awaitingPics.isNotEmpty()) {
                Timber.i("Resuming PICS for ${awaitingPics.size} apps still without app info")
                awaitingPics.forEach { requestAppInfo(it) }
            }
        }
    }

    /** Polls the PICS change list; [onPicsChanges] handles the answer. */
    private fun CoroutineScope.continuousPicsChanges() = launch {
        while (isActive) {
            delay(1.minutes)

            val lastChangeNumber = account.lastChangeNumber.first() ?: 0

            Timber.d("picsGetChangesSince($lastChangeNumber)")

            steamApps.picsGetChangesSince(
                lastChangeNumber = lastChangeNumber,
                sendAppChangeList = true,
                sendPackageChangelist = true,
            )
        }
    }

    /** Drains [picsRequestChannel] into batched app info requests. */
    private fun CoroutineScope.continuousPicsChecker() = launch {
        picsRequestChannel.receiveAsFlow()
            // Long enough to batch a burst (a library ingest fills 500 well within it), short
            // enough that a single lookup, such as a friend's unknown game, is not held up.
            .timeChunked(500, 2.seconds)
            .collect { ids ->
                Timber.d("Collected ${ids.size} app(s) to query PICS")
                steamApps.picsGetProductInfo(
                    apps = ids.map { PICSRequest(id = it, accessToken = 0) },
                    packages = emptyList(),
                )
            }
    }

    /** Copies playtime and last-played from `Player.GetOwnedGames` onto the stored apps. */
    private fun CoroutineScope.syncPlaytime() = launch {
        val games = runCatching { library.ownedGames() }
            .onFailure { Timber.w(it, "Playtime sync failed") }
            .getOrNull() ?: return@launch

        val appDao = db.steamAppDao()
        db.withTransaction {
            games.forEach { game ->
                appDao.updatePlaytime(
                    game.appId,
                    game.playtimeForever,
                    game.playtimeTwoWeeks,
                    game.lastPlayed,
                )
            }
        }
        Timber.d("Synced playtime for ${games.size} games")
    }

    private companion object {
        /** Max ids per `WHERE id IN (...)` query, kept well under SQLite's bound-parameter limit. */
        const val SQL_IN_CHUNK_SIZE = 500
    }
}
