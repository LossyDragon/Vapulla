package `in`.dragonbra.vapulla.service.download

import android.os.Build
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientGetClientAppListResponse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientGetClientDetailsResponse
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.DEVICE_NAME
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.service.callback.RemoteAppListRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteDownloadsSwitchRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteInstallRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteUninstallRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteUpdateStateRequestCallback
import `in`.dragonbra.vapulla.service.handler.VapullaHandler
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Answers the account's other devices when they ask what this one has: the other end of the
 * Remote Clients screen. With it Vapulla shows up as a client of its own in Steam's
 * `ClientComm` service, with the account's games and how those on its Downloads list stand.
 *
 * A request to download a game is queued like one made on the game's own page, for
 * Windows, the public branch and English, and a download can be paused and carried on
 * with from the other device, and a game can be removed from it, files and all. A request
 * to start a game is not acted on.
 */
class RemoteRequests(
    private val scope: CoroutineScope,
    private val handler: VapullaHandler,
    private val appDao: SteamAppDao,
    private val downloadDao: DownloadDao,
    private val licenseDao: SteamLicenseDao,
    private val downloads: DownloadManager,
) : SessionComponent {

    /** The downloads stopped by [switchDownloads], as app and workshop item ID. */
    private val pausedForAnotherDevice = ConcurrentHashMap.newKeySet<Pair<Int, Long>>()

    /**
     * When each game was removed for another device. Like a game that just finished, one
     * that was just removed is kept in the answers about what is changing for a while, or
     * the other device would go on showing it as installed.
     */
    private val removedAt = ConcurrentHashMap<Int, Long>()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribe(RemoteUninstallRequestCallback::class.java) { request ->
            scope.launch { uninstall(request) }
        },
        callbacks.subscribe(RemoteInstallRequestCallback::class.java) { request ->
            scope.launch { install(request) }
        },
        callbacks.subscribe(RemoteUpdateStateRequestCallback::class.java) { request ->
            setUpdateState(request)
        },
        callbacks.subscribe(RemoteDownloadsSwitchRequestCallback::class.java) { request ->
            scope.launch { switchDownloads(request) }
        },
        callbacks.subscribe(RemoteAppListRequestCallback::class.java) { request ->
            scope.launch { answer(request) }
        },
    )

    private suspend fun answer(request: RemoteAppListRequestCallback) {
        val downloaded = downloadDao.observeApps().first().associateBy { it.appId }

        // Steam's own client offers a device for a game only when the game is in the
        // device's list, so the whole library is answered: what is on the Downloads list
        // as it stands there, and every other owned game as not installed.
        val appIds = when {
            request.onlyChanging ->
                downloaded.values
                    .filter { it.isChanging || it.justFinished || it.state == DownloadState.PAUSED }
                    .map { it.appId }
                    // One removed and queued again since is on the list, and listed once.
                    .let { changing -> (changing + justRemoved()).distinct() }

            request.onlyInstalled ->
                downloaded.values.filter { it.state == DownloadState.COMPLETED }.map { it.appId }

            else -> (appDao.ownedGameIds() + downloaded.keys).distinct()
        }.filter { request.appIds.isEmpty() || it in request.appIds }

        Timber.d("Another device asked for the games here; answering with ${appIds.size}")

        val response = CMsgClientGetClientAppListResponse.newBuilder()
            .setBytesAvailable(downloads.freeBytes())
            .addAllApps(appIds.map { downloaded[it]?.toRemoteApp() ?: notInstalled(it) })

        if (request.includeClientInfo) {
            response.setClientInfo(
                CMsgClientGetClientDetailsResponse.newBuilder()
                    .setOs("Android ${Build.VERSION.RELEASE}")
                    .setMachineName(DEVICE_NAME)
                    .setBytesAvailable(downloads.freeBytes())
                    .setProtocolVersion(PROTOCOL_VERSION)
                    .setClientcommVersion(CLIENTCOMM_VERSION),
            )
        }

        handler.sendClientAppList(request.requestJobId, response)
    }

    /**
     * Queues the game another device asked for. The answer only says whether it was queued;
     * how the download gets on, the other device reads from the next lists it asks for.
     */
    private suspend fun install(request: RemoteInstallRequestCallback) {
        val app = appDao.findApp(request.appId)
        // The first plan is the one the game's page offers first: Windows, when there is one.
        val plan = app?.let { DownloadPlanner.plans(it, licenseDao.ownedDepotIds()).firstOrNull() }

        val problem = when {
            app == null -> "the app is not in the library"
            plan == null -> "there is nothing to download"
            else -> downloads.enqueue(app, plan)
        }

        if (problem == null) {
            Timber.i("Queued app ${request.appId} for another device")
        } else {
            Timber.w("Not queuing app ${request.appId} for another device: $problem")
        }
        handler.sendInstallResult(
            request.requestJobId,
            if (problem == null) EResult.OK else EResult.Fail,
        )
    }

    /**
     * Takes the game off the Downloads list and its files off the disk, as the Remove button
     * there does. A game that is not on the list is answered with a failure.
     */
    private suspend fun uninstall(request: RemoteUninstallRequestCallback) {
        val onList = downloadDao.find(request.appId) != null
        Timber.i("Another device asked to remove app ${request.appId}; on the list: $onList")

        if (onList) {
            downloads.remove(request.appId, deleteFiles = true)
            removedAt[request.appId] = System.currentTimeMillis()
        }
        handler.sendUninstallResult(
            request.requestJobId,
            if (onList) EResult.OK else EResult.Fail,
        )
    }

    private fun setUpdateState(request: RemoteUpdateStateRequestCallback) {
        Timber.i(
            "Another device asked to ${if (request.update) "carry on with" else "pause"} " +
                "app ${request.appId}",
        )

        if (request.update) downloads.resume(request.appId) else downloads.pause(request.appId)
        handler.sendUpdateStateResult(request.requestJobId, EResult.OK)
    }

    /**
     * Stops every download that is running or waiting, or carries on with the ones stopped
     * this way. A download the user paused on this device stays paused.
     */
    private suspend fun switchDownloads(request: RemoteDownloadsSwitchRequestCallback) {
        Timber.i("Another device switched downloads ${if (request.enable) "on" else "off"}")

        if (request.enable) {
            pausedForAnotherDevice.forEach { (appId, workshopId) ->
                downloads.resume(appId, workshopId)
            }
            pausedForAnotherDevice.clear()
        } else {
            downloadDao.observeAll().first().filter { it.isChanging }.forEach {
                pausedForAnotherDevice += it.appId to it.workshopId
                downloads.pause(it.appId, it.workshopId)
            }
        }
        handler.sendDownloadsSwitchResult(request.requestJobId, EResult.OK)
    }

    private fun justRemoved(): List<Int> {
        val now = System.currentTimeMillis()
        removedAt.values.removeIf { now - it >= JUST_FINISHED.inWholeMilliseconds }
        return removedAt.keys.toList()
    }

    private val Download.isChanging: Boolean
        get() = state == DownloadState.QUEUED || state == DownloadState.DOWNLOADING

    /**
     * Finished a moment ago. While a game downloads, the other device asks only for what is
     * changing, so a game that dropped out of that answer the instant it finished would
     * stay at its last percentage over there. It is kept in for a little longer, as done.
     */
    private val Download.justFinished: Boolean
        get() = state == DownloadState.COMPLETED &&
            System.currentTimeMillis() - finishedAt < JUST_FINISHED.inWholeMilliseconds

    /** An owned game that is not on the Downloads list. Its size is not known until asked for. */
    private fun notInstalled(appId: Int): CMsgClientGetClientAppListResponse.App =
        CMsgClientGetClientAppListResponse.App.newBuilder()
            .setAppid(appId)
            .setAppType("game")
            .setInstalled(false)
            .setAvailableOnPlatform(true)
            .setQueuePosition(-1)
            .build()

    /**
     * The game as the other device should see it. A download that is running is reported
     * from the live figures of [DownloadManager.progress]; the row only has a snapshot of
     * them that is saved now and then.
     */
    private fun Download.toRemoteApp(): CMsgClientGetClientAppListResponse.App {
        val live = downloads.progress.value?.takeIf { it.appId == appId && it.workshopId == 0L }
        val done = state == DownloadState.COMPLETED
        val bytes = if (done) totalBytes else live?.downloadedBytes ?: downloadedBytes
        val rate = live?.bytesPerSecond ?: 0L

        return CMsgClientGetClientAppListResponse.App.newBuilder()
            .setAppid(appId)
            .setAppType("game")
            .setInstalled(done)
            .setChanging(isChanging)
            .setDownloadPaused(state == DownloadState.PAUSED)
            .setNumDownloading(if (state == DownloadState.DOWNLOADING) 1 else 0)
            .setBytesDownloaded(bytes)
            .setBytesToDownload(totalBytes)
            .setBytesDownloadRate(rate.toInt())
            .setEstimatedSecondsRemaining(
                if (rate > 0) ((totalBytes - bytes).coerceAtLeast(0) / rate).toInt() else 0,
            )
            .setBytesRequired(if (done) 0 else totalBytes)
            .setQueuePosition(if (isChanging) 0 else -1)
            .setAvailableOnPlatform(true)
            .build()
    }

    private companion object {
        // What Steam's own clients report in these two fields.
        const val PROTOCOL_VERSION = 65581
        const val CLIENTCOMM_VERSION = 1

        /** Longer than the other device takes to ask again: 10 s while a download runs, 60 after. */
        val JUST_FINISHED = 2.minutes
    }
}
