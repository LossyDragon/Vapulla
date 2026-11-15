package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.vapulla.util.appendCapped
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/**
 * An app or package that Steam reported as changed. [id] is a running number, used as the
 * list key.
 *
 * @property targetId the app ID or package ID.
 * @property changeNumber the global change number this edit was given.
 * @property needsToken Steam only hands out this one's details to accounts with access to it.
 * @property name the app's name when the database knows it; packages have none.
 * @property inLibrary the app or package is one of ours, so the library sync acts on it.
 */
@Immutable
data class PicsChange(
    val id: Long,
    val timestamp: Long,
    val isApp: Boolean,
    val targetId: Int,
    val changeNumber: Int,
    val needsToken: Boolean,
    val name: String,
    val inLibrary: Boolean,
)

/** Where the feed stands. [changeNumber] is 0 until the first answer arrives. */
@Immutable
data class PicsFeedStatus(
    val changeNumber: Int = 0,
    val checkedAt: Long = 0,
    val checks: Int = 0,
    val note: String? = null,
)

/**
 * Every change to Steam's app and package catalogue (PICS) seen in this run, for the Under
 * the Hood screen. Fed by `LicenseSync`, which polls `picsGetChangesSince` anyway to keep the
 * library up to date and otherwise discards whatever is not in it.
 *
 * The feed says that something changed, never what: that takes fetching the app info before
 * and after and comparing the two.
 */
object PicsFeed {

    private const val MAX_ENTRIES = 1000

    /** Often enough to keep the library up to date. */
    private val IDLE_POLL_INTERVAL = 1.minutes

    /** Often enough to watch changes come in. */
    private val WATCHED_POLL_INTERVAL = 5.seconds

    private val nextId = AtomicLong()

    private val _status = MutableStateFlow(PicsFeedStatus())
    val status: StateFlow<PicsFeedStatus> = _status.asStateFlow()

    private val _entries = MutableStateFlow<ImmutableList<PicsChange>>(persistentListOf())

    /** Changes seen, oldest first. */
    val entries: StateFlow<ImmutableList<PicsChange>> = _entries.asStateFlow()

    /**
     * True while the feed is on screen. The poll then runs every few seconds instead of once
     * a minute, which is often enough for the library but too slow to watch.
     */
    val watching = MutableStateFlow(false)

    fun clear() {
        _entries.value = persistentListOf()
    }

    /** A poll was answered: Steam's catalogue is now at [changeNumber]. */
    internal fun checked(changeNumber: Int, note: String?) {
        _status.update {
            PicsFeedStatus(
                changeNumber = changeNumber,
                checkedAt = System.currentTimeMillis(),
                checks = it.checks + 1,
                note = note,
            )
        }
    }

    internal fun add(changes: List<Change>) {
        val now = System.currentTimeMillis()
        val added = changes.sortedBy { it.changeNumber }.map { change ->
            PicsChange(
                id = nextId.getAndIncrement(),
                timestamp = now,
                isApp = change.isApp,
                targetId = change.targetId,
                changeNumber = change.changeNumber,
                needsToken = change.needsToken,
                name = change.name,
                inLibrary = change.inLibrary,
            )
        }
        _entries.appendCapped(added, MAX_ENTRIES)
    }

    /**
     * Takes in Steam's answer to a `picsGetChangesSince` poll: every change in it, and where
     * the catalogue now stands.
     *
     * @param appNames the names of the changed apps that are known, by app ID.
     * @param ownedApps the changed apps that are in the library.
     * @param ownedPackages the changed packages the account has a license for.
     */
    internal fun report(
        callback: PICSChangesCallback,
        appNames: Map<Int, String> = emptyMap(),
        ownedApps: Set<Int> = emptySet(),
        ownedPackages: Set<Int> = emptySet(),
    ) {
        val apps = callback.appChanges.values.map { change ->
            Change(
                isApp = true,
                targetId = change.id,
                changeNumber = change.changeNumber,
                needsToken = change.isNeedsToken,
                name = appNames[change.id].orEmpty(),
                inLibrary = change.id in ownedApps,
            )
        }
        val packages = callback.packageChanges.values.map { change ->
            Change(
                isApp = false,
                targetId = change.id,
                changeNumber = change.changeNumber,
                needsToken = change.isNeedsToken,
                name = "",
                inLibrary = change.id in ownedPackages,
            )
        }

        add(apps + packages)
        checked(
            changeNumber = callback.currentChangeNumber,
            // Steam stops listing changes when the last number it was given is too old.
            note = "Too far behind for a list of changes; the next check will have one."
                .takeIf { callback.isRequiresFullUpdate },
        )
    }

    /**
     * Waits until the next poll is due: [WATCHED_POLL_INTERVAL] while the feed is on screen,
     * else [IDLE_POLL_INTERVAL], which opening the feed cuts short.
     */
    internal suspend fun awaitNextPoll() {
        if (watching.value) {
            delay(WATCHED_POLL_INTERVAL)
        } else {
            withTimeoutOrNull(IDLE_POLL_INTERVAL) { watching.first { it } }
        }
    }

    /** A change as the sync hands it over, before it is given an ID and a time. */
    internal class Change(
        val isApp: Boolean,
        val targetId: Int,
        val changeNumber: Int,
        val needsToken: Boolean,
        val name: String,
        val inLibrary: Boolean,
    )
}
