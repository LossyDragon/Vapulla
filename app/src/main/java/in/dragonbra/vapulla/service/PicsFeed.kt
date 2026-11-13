package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        if (changes.isEmpty()) return

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
        _entries.update { current -> (current + added).takeLast(MAX_ENTRIES).toImmutableList() }
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
