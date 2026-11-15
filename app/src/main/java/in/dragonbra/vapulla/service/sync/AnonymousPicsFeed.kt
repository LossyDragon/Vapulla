package `in`.dragonbra.vapulla.service.sync

import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.service.PicsFeed
import `in`.dragonbra.vapulla.service.SessionComponent
import java.io.Closeable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Feeds [PicsFeed] during an anonymous session. Signed in, [LicenseSync] does that on the
 * side while it keeps the library up to date; without an account there is no library, so
 * this only polls `picsGetChangesSince` and passes on what comes back. Nothing is stored.
 */
class AnonymousPicsFeed(private val steamApps: SteamApps) : SessionComponent {

    /** Where Steam's catalogue stood at the last answer; 0 asks for where it stands now. */
    @Volatile
    private var lastChangeNumber = 0

    override fun subscribe(callbacks: CallbackManager): List<Closeable> =
        listOf(callbacks.subscribe(onPicsChanges))

    override fun start(session: CoroutineScope) {
        session.launch {
            while (isActive) {
                steamApps.picsGetChangesSince(
                    lastChangeNumber = lastChangeNumber,
                    sendAppChangeList = true,
                    sendPackageChangelist = true,
                )

                // Every few seconds while the feed is on screen, else once a minute; opening
                // the feed cuts the wait short.
                if (PicsFeed.watching.value) {
                    delay(FEED_POLL_INTERVAL)
                } else {
                    withTimeoutOrNull(1.minutes) { PicsFeed.watching.first { it } }
                }
            }
        }
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer { callback ->
        lastChangeNumber = callback.currentChangeNumber

        val apps = callback.appChanges.values.map { change ->
            PicsFeed.Change(
                isApp = true,
                targetId = change.id,
                changeNumber = change.changeNumber,
                needsToken = change.isNeedsToken,
                name = "",
                inLibrary = false,
            )
        }
        val packages = callback.packageChanges.values.map { change ->
            PicsFeed.Change(
                isApp = false,
                targetId = change.id,
                changeNumber = change.changeNumber,
                needsToken = change.isNeedsToken,
                name = "",
                inLibrary = false,
            )
        }

        PicsFeed.add(apps + packages)
        PicsFeed.checked(
            changeNumber = callback.currentChangeNumber,
            // Steam stops listing changes when the last number it was given is too old.
            note = "Too far behind for a list of changes; the next check will have one."
                .takeIf { callback.isRequiresFullUpdate },
        )
    }

    private companion object {
        val FEED_POLL_INTERVAL = 5.seconds
    }
}
