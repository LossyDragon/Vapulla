package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.protobufs.webui.ServiceCloudconfigstore.CCloudConfigStore_Change_Notification
import `in`.dragonbra.javasteam.protobufs.webui.ServiceCloudconfigstore.CCloudConfigStore_Download_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceCloudconfigstore.CCloudConfigStore_NamespaceVersion
import `in`.dragonbra.javasteam.rpc.service.webui.CloudConfigStore
import `in`.dragonbra.javasteam.rpc.service.webui.CloudConfigStoreClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.util.LenientJson
import java.io.Closeable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * One of the collections the account sorts its library into in the Steam client.
 *
 * @property id Steam's name for it: "favorite" and "hidden" for the two every account has,
 *   "uc-" and some letters for one the user made.
 * @property appIds the apps put in it by hand.
 */
@Immutable
data class LibraryCollection(val id: String, val name: String, val appIds: ImmutableSet<Int>)

/** The account's library collections. Held in memory only: read from Steam at every sign-in. */
object SteamCollections {

    private val _collections = MutableStateFlow<ImmutableList<LibraryCollection>>(persistentListOf())

    /** Favorites first, then by name; empty while signed out. Ones without apps are left out. */
    val collections: StateFlow<ImmutableList<LibraryCollection>> = _collections.asStateFlow()

    internal fun set(collections: List<LibraryCollection>) {
        _collections.value = collections
            .sortedWith(
                compareBy<LibraryCollection> { it.id != CollectionsApi.FAVORITES_ID }
                    .thenBy { it.name.lowercase() },
            )
            .toImmutableList()
    }
}

/**
 * Library collections via the `CloudConfigStore` service, from JavaSteam's
 * `javasteam-protobufs-webui` artifact. The Steam client keeps settings that follow the
 * account in that store, as keys with a text value, grouped in numbered namespaces; the
 * collections are in namespace 1, one key each, with JSON as the value. They are read at
 * sign-in and again when Steam says the namespace changed. Nothing is written.
 *
 * A dynamic collection is a saved filter that the Steam client works out by itself; only
 * the apps added to it by hand are stored, so only those are in it here.
 */
class CollectionsApi(private val cloudConfig: CloudConfigStore) : SessionComponent {

    /** The value of one collection's key, as far as it is read. */
    @Serializable
    private class Stored(
        val id: String = "",
        val name: String = "",
        val added: List<Int> = emptyList(),
    )

    /** The sign-in under way, for reloading when Steam says the collections changed. */
    @Volatile
    private var session: CoroutineScope? = null

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            CloudConfigStoreClient,
            CCloudConfigStore_Change_Notification.Builder,
            > { notification ->
            if (notification.body.versionsList.any { it.enamespace == LIBRARY_NAMESPACE }) {
                Timber.d("Library collections changed on Steam")
                session?.launch { load() }
            }
        },
    )

    override fun start(session: CoroutineScope) {
        this.session = session
        session.launch { load() }
    }

    private suspend fun load() {
        // Version 0 asks for everything rather than what changed since a version held.
        val request = CCloudConfigStore_Download_Request.newBuilder()
            .addVersions(
                CCloudConfigStore_NamespaceVersion.newBuilder()
                    .setEnamespace(LIBRARY_NAMESPACE)
                    .setVersion(0),
            )
            .build()

        val body = cloudConfig.download(request).bodyOrNull("CloudConfigStore.Download") ?: return

        val collections = body.dataList
            .filter { it.enamespace == LIBRARY_NAMESPACE }
            .flatMap { it.entriesList }
            .filter { !it.isDeleted && it.key.startsWith(KEY_PREFIX) }
            .mapNotNull { entry ->
                runCatching { LenientJson.decodeFromString<Stored>(entry.value) }
                    .onFailure { Timber.w(it, "Unreadable collection ${entry.key}") }
                    .getOrNull()
            }
            .filter { it.added.isNotEmpty() }
            .map { stored ->
                val id = stored.id.ifEmpty { "?" }
                LibraryCollection(
                    id = id,
                    name = stored.name.ifEmpty { BUILT_IN_NAMES[id] ?: id },
                    appIds = stored.added.toImmutableSet(),
                )
            }

        Timber.d("Loaded ${collections.size} library collections")
        SteamCollections.set(collections)
    }

    internal companion object {
        const val FAVORITES_ID = "favorite"

        /** The namespace the Steam client keeps the library's settings in. */
        private const val LIBRARY_NAMESPACE = 1

        private const val KEY_PREFIX = "user-collections."

        /** The two collections every account has are stored without a name. */
        private val BUILT_IN_NAMES = mapOf(FAVORITES_ID to "Favorites", "hidden" to "Hidden")
    }
}
