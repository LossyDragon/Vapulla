package `in`.dragonbra.vapulla.service.sync

import androidx.room.withTransaction
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
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
import `in`.dragonbra.vapulla.service.PicsFeed
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.service.api.FamilyApi
import `in`.dragonbra.vapulla.service.api.LibraryApi
import `in`.dragonbra.vapulla.service.api.awaitOrNull
import `in`.dragonbra.vapulla.util.generateSteamApp
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import `in`.dragonbra.vapulla.util.runSuspendCatching
import `in`.dragonbra.vapulla.util.timeChunked
import java.io.Closeable
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val family: FamilyApi,
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
        session.syncFamilyLibrary()
    }

    /** Queues a PICS app info request for [appId]; the reply is stored like any other. */
    fun requestAppInfo(appId: Int) {
        // The channel is unlimited, so this never fails for lack of room.
        picsRequestChannel.trySend(appId)
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer {
        scope.launch {
            // Before anything is filtered out: the feed shows all of it.
            feedChanges(it)

            // The usual answer: nothing changed since the last poll.
            if (account.lastChangeNumber.first() == it.currentChangeNumber) {
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
                    .awaitOrNull("PICSGetAccessTokens") ?.packageTokens.orEmpty()
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
                    .asSequence()
                    .map { info -> info.packageId }
                    .filter { packageId -> packageId != SteamApp.NO_PACKAGE }
                    .distinct()
                    .chunked(SQL_IN_CHUNK_SIZE)
                    .toList() // Sequence lambdas can't suspend, so query from a list.
                    .flatMap { ids -> licenseDao.findLicenseInfo(ids) }
                    .associateBy { license -> license.packageID }

                val apps = callback.apps.values.mapNotNull { app ->
                    val changeInfo = changeInfoById[app.id]
                    if (app.changeNumber == changeInfo?.lastChangeNumber) {
                        return@mapNotNull null
                    }

                    val packageId = changeInfo?.packageId ?: SteamApp.NO_PACKAGE
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

        if (it.result != EResult.OK) {
            Timber.w("Failed to get license list: ${it.result}")
            return@Consumer
        }

        scope.launch {
            delay(5.seconds)

            val stored = db.steamLicenseDao().getAllLicenses()
            val changed = changedLicenses(incoming = it.licenseList, stored = stored)

            if (changed.isNotEmpty()) {
                Timber.i("Adding/updating ${changed.size} licenses")
                db.steamLicenseDao().insert(changed)
            }

            val incomingIds = it.licenseList.mapTo(HashSet()) { license -> license.packageID }
            removeLicenses(stored.filter { license -> license.packageID !in incomingIds })

            // Only new and changed licenses need their package info fetched.
            if (changed.isNotEmpty()) {
                val picsRequests = changed.map { license ->
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
                awaitingPics.forEach(::requestAppInfo)
            }
        }
    }

    /**
     * The licenses in [incoming] that are new or have changed since they were [stored], as the
     * rows to store. A package can be licensed to several accounts (family sharing): the
     * details are taken from our own copy when there is one, the owners and flags from all.
     */
    private fun changedLicenses(
        incoming: List<License>,
        stored: List<SteamLicense>,
    ): List<SteamLicense> {
        val storedById = stored.associateBy { license -> license.packageID }
        val myAccountId = localSteamId()?.accountID?.toInt()

        return incoming
            .groupBy { license -> license.packageID }
            .mapNotNull { (packageId, copies) ->
                val license = copies.firstOrNull { copy -> copy.ownerAccountID == myAccountId }
                    ?: copies.first()
                val old = storedById[packageId]

                if (old != null && old.lastChangeNumber == license.lastChangeNumber) {
                    return@mapNotNull null
                }

                SteamLicense(
                    packageID = packageId,
                    lastChangeNumber = license.lastChangeNumber,
                    timeCreated = license.timeCreated,
                    timeNextProcess = license.timeNextProcess,
                    minuteLimit = license.minuteLimit,
                    minutesUsed = license.minutesUsed,
                    paymentMethod = license.paymentMethod,
                    licenseFlags = copies.fold(emptyEnumSet()) { flags, copy ->
                        flags.apply { addAll(copy.licenseFlags) }
                    },
                    purchaseCode = license.purchaseCode,
                    licenseType = license.licenseType,
                    territoryCode = license.territoryCode,
                    accessToken = license.accessToken,
                    ownerAccountID = copies.map { copy -> copy.ownerAccountID },
                    masterPackageID = license.masterPackageID,
                    // The package info that was fetched for the old row still applies.
                    appIds = old?.appIds ?: emptyList(),
                    depotIds = old?.depotIds ?: emptyList(),
                )
            }
    }

    /**
     * Deletes licenses the account no longer has. The apps they granted stay in the table,
     * since friends may be playing them, but no longer count as in the library.
     */
    private suspend fun removeLicenses(licenses: List<SteamLicense>) {
        if (licenses.isEmpty()) return
        Timber.i("Removing ${licenses.size} stale licenses")

        val appDao = db.steamAppDao()
        db.withTransaction {
            licenses.forEach { license ->
                license.appIds.forEach { appId ->
                    val app = appDao.findApp(appId)
                    // Left alone when it has since been put under another package.
                    if (app != null && app.packageId == license.packageID) {
                        appDao.update(
                            app.copy(
                                packageId = SteamApp.NO_PACKAGE,
                                ownerAccountId = emptyList(),
                                licenseFlags = emptyEnumSet(),
                            ),
                        )
                    }
                }
            }

            db.steamLicenseDao().deleteStaleLicenses(licenses.map { it.packageID })
        }
    }

    /** Hands everything in a change list to [PicsFeed], with names where they are known. */
    private suspend fun feedChanges(callback: PICSChangesCallback) {
        val appDao = db.steamAppDao()
        val appIdChunks = callback.appChanges.keys.chunked(SQL_IN_CHUNK_SIZE)

        val appNames = appIdChunks
            .flatMap { ids -> appDao.findNames(ids) }
            .associate { app -> app.id to app.name }

        // An app can be in the database without being ours: friends' games are stored too.
        val ownedApps = appIdChunks
            .flatMap { ids -> appDao.findChangeInfo(ids) }
            .filter { info -> info.packageId != SteamApp.NO_PACKAGE }
            .mapTo(HashSet()) { info -> info.id }

        val ownedPackages = callback.packageChanges.keys.chunked(SQL_IN_CHUNK_SIZE)
            .flatMap { ids -> db.steamLicenseDao().findLicenseInfo(ids) }
            .mapTo(HashSet()) { info -> info.packageID }

        PicsFeed.report(callback, appNames, ownedApps, ownedPackages)
    }

    /**
     * Polls the PICS change list; [onPicsChanges] handles the answer. How often is up to
     * [PicsFeed.awaitNextPoll]: seldom for the library alone, often while the feed is watched.
     */
    private fun CoroutineScope.continuousPicsChanges() = launch {
        while (isActive) {
            PicsFeed.awaitNextPoll()

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
            .timeChunked(PICS_BATCH_SIZE, PICS_BATCH_WINDOW)
            .collect { ids ->
                Timber.d("Collected ${ids.size} app(s) to query PICS")

                // Some apps only give up their full info for a token, which Steam hands
                // out for the apps the account may see. Without an answer, ask without.
                val tokens = steamApps
                    .picsGetAccessTokens(appIds = ids, packageIds = emptyList())
                    .awaitOrNull("PICSGetAccessTokens")
                    ?.appTokens
                    .orEmpty()

                steamApps.picsGetProductInfo(
                    apps = ids.map { PICSRequest(id = it, accessToken = tokens[it] ?: 0) },
                    packages = emptyList(),
                )
            }
    }

    /** Copies playtime and last-played from `Player.GetOwnedGames` onto the stored apps. */
    private fun CoroutineScope.syncPlaytime() = launch {
        val games = runSuspendCatching { library.ownedGames() }
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

    /**
     * Makes sure every game the Steam Family shares is in the library. Most are there
     * already, because Steam lists the family's licenses with the account's own; this adds
     * the ones no license brought in, and takes out the ones that are no longer shared.
     * Apps new to the table show up once their app info has arrived.
     */
    private fun CoroutineScope.syncFamilyLibrary() = launch {
        val shared = runSuspendCatching { family.family()?.let { family.sharedLibrary(it.id) } }
            .onFailure { Timber.w(it, "Family library sync failed") }
            .getOrNull()
            ?: return@launch

        val appDao = db.steamAppDao()
        val sharedIds = shared.map { it.appId }

        val known = sharedIds
            .chunked(SQL_IN_CHUNK_SIZE)
            .flatMap { ids -> appDao.findChangeInfo(ids) }
            .associateBy { info -> info.id }

        // No license of ours grants these, so nothing else would put them in the library.
        val toAdd = sharedIds.filter { id ->
            (known[id]?.packageId ?: SteamApp.NO_PACKAGE) == SteamApp.NO_PACKAGE
        }
        val noLongerShared = appDao.findAppIdsOfPackage(FAMILY_PKG_ID) - sharedIds.toSet()

        db.withTransaction {
            appDao.insertIfAbsent(toAdd.map { SteamApp(id = it, packageId = FAMILY_PKG_ID) })
            toAdd.chunked(SQL_IN_CHUNK_SIZE).forEach { appDao.updatePackageId(FAMILY_PKG_ID, it) }
            noLongerShared.chunked(SQL_IN_CHUNK_SIZE)
                .forEach { appDao.updatePackageId(SteamApp.NO_PACKAGE, it) }
        }

        toAdd.forEach { requestAppInfo(it) }
        Timber.d(
            "Family library: %d shared, %d added, %d removed",
            sharedIds.size,
            toAdd.size,
            noLongerShared.size,
        )
    }

    private companion object {
        /** Max ids per `WHERE id IN (...)` query, kept well under SQLite's bound-parameter limit. */
        const val SQL_IN_CHUNK_SIZE = 500

        /**
         * The package ID stored for an app that is in the library only because the Steam
         * Family shares it. Not a real package: no license is looked up for it.
         */
        const val FAMILY_PKG_ID = Int.MAX_VALUE - 1

        /** Most app IDs sent in one PICS app info request. */
        const val PICS_BATCH_SIZE = 500

        /**
         * How long app IDs are collected for one request: long enough to batch a burst (a
         * library ingest fills a batch well within it), short enough that a single lookup,
         * such as a friend's unknown game, is not held up.
         */
        val PICS_BATCH_WINDOW = 2.seconds
    }
}
