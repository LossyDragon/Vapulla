package `in`.dragonbra.vapulla.service.sync

import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.service.PicsFeed
import `in`.dragonbra.vapulla.service.SessionComponent
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
            // Asks straight away: nothing is on screen until the first answer.
            while (isActive) {
                steamApps.picsGetChangesSince(
                    lastChangeNumber = lastChangeNumber,
                    sendAppChangeList = true,
                    sendPackageChangelist = true,
                )
                PicsFeed.awaitNextPoll()
            }
        }
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer { callback ->
        lastChangeNumber = callback.currentChangeNumber
        PicsFeed.report(callback)
    }
}
