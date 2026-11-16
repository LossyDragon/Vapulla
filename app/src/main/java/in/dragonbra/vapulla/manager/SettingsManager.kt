package `in`.dragonbra.vapulla.manager

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** User-tunable values. Defaults are the values the app shipped with before Settings existed. */
@Immutable
data class AppSettings(
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = false,
    /** Servers asked of the master server per query. */
    val maxServers: Int = 50,
    /** How long to wait for each server's A2S_INFO reply. */
    val serverTimeoutMs: Int = 1500,
    val workshopPageSize: Int = 20,
    /** Packets kept in the in-app network log. */
    val networkLogSize: Int = 500,
    /**
     * Whether the network log keeps packet bodies, to look at one in detail. Off by default:
     * bodies hold chat messages and the like, and stay in memory until the log is cleared.
     */
    val networkLogBodies: Boolean = false,
    /** How many days back a chat counts as recent in the friends list. */
    val recentChatsDays: Int = 7,
    /**
     * How a friend with a nickname is listed: "Name (Nickname)" when true, otherwise the
     * nickname alone, marked with an asterisk.
     */
    val appendNicknames: Boolean = true,
    /** Whether friends you have blocked get a section of their own in the friends list. */
    val showBlockedFriends: Boolean = false,
    /** Shows chat messages as the BBCode Steam sent instead of rendering it. */
    val rawBBCode: Boolean = false,
    /** Steam's community and store pages open inside the app, signed in to the account. */
    val signedInWebPages: Boolean = false,
    /** Downloads wait while the device is on mobile data or another metered network. */
    val downloadWifiOnly: Boolean = false,
    /** Megabytes per second downloads are held to; 0 for no limit. */
    val downloadSpeedLimit: Int = 0,
    /** Chunks fetched at the same time, each over its own connection. */
    val downloadConnections: Int = 8,
    /** Chunks written to disk at the same time. */
    val downloadFileWrites: Int = 1,
    /** Threads that decrypt and decompress chunks; 0 for half the cores. */
    val downloadDecompressThreads: Int = 0,
    /**
     * Folder new downloads are installed under, one of the storage volumes Android gives the
     * app. Empty for the default, the device's own storage.
     */
    val downloadLocation: String = "",
    /**
     * What kind of device the app tells Steam it is at sign-in, a code of JavaSteam's
     * `EGamingDeviceType`. The account's other Steam clients list their fellow clients by
     * it, and name only a few kinds: as a PC the app is listed as "PC Client", as a phone
     * under a text key Steam never translated, and without a kind not at all.
     */
    val reportedDeviceType: Int = DEVICE_TYPE_PC,
) {
    companion object {
        val RECENT_CHATS_DAYS = listOf(1, 5, 7, 30)
        val MAX_SERVERS_RANGE = 25..500
        const val MAX_SERVERS_STEP = 25
        val SERVER_TIMEOUT_RANGE = 500..5000
        const val SERVER_TIMEOUT_STEP = 250
        val WORKSHOP_PAGE_SIZES = listOf(10, 20, 50, 100)
        val NETWORK_LOG_SIZES = listOf(100, 250, 500, 1000)
        val DOWNLOAD_SPEED_LIMITS = listOf(0, 1, 2, 5, 10)
        val DOWNLOAD_CONNECTIONS = listOf(4, 8, 16, 32, 64)
        val DOWNLOAD_FILE_WRITES = listOf(1, 2, 4, 8, 16)
        val DOWNLOAD_DECOMPRESS_THREADS = listOf(0, 2, 4, 6, 8)

        const val DEVICE_TYPE_PC = 1
        const val DEVICE_TYPE_PHONE = 528
        const val DEVICE_TYPE_NONE = 0
        val REPORTED_DEVICE_TYPES = listOf(DEVICE_TYPE_PC, DEVICE_TYPE_PHONE, DEVICE_TYPE_NONE)
    }
}

/**
 * The current [AppSettings], provided once at the root so leaf composables (a friend's name,
 * say) can follow a setting without it being passed down through every screen.
 */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/** Persists [AppSettings] in their own DataStore, separate from the account preferences. */
class SettingsManager(private val context: Context) {

    private companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        val DARK_THEME = Stored(booleanPreferencesKey("dark_theme")) { it.darkTheme }
        val DYNAMIC_COLOR = Stored(booleanPreferencesKey("dynamic_color")) { it.dynamicColor }
        val MAX_SERVERS = Stored(intPreferencesKey("max_servers")) { it.maxServers }
        val SERVER_TIMEOUT_MS =
            Stored(intPreferencesKey("server_timeout_ms")) { it.serverTimeoutMs }
        val WORKSHOP_PAGE_SIZE =
            Stored(intPreferencesKey("workshop_page_size")) { it.workshopPageSize }
        val NETWORK_LOG_SIZE = Stored(intPreferencesKey("network_log_size")) { it.networkLogSize }
        val NETWORK_LOG_BODIES =
            Stored(booleanPreferencesKey("network_log_bodies")) { it.networkLogBodies }
        val RECENT_CHATS_DAYS =
            Stored(intPreferencesKey("recent_chats_days")) { it.recentChatsDays }
        val APPEND_NICKNAMES =
            Stored(booleanPreferencesKey("append_nicknames")) { it.appendNicknames }
        val SHOW_BLOCKED_FRIENDS =
            Stored(booleanPreferencesKey("show_blocked_friends")) { it.showBlockedFriends }
        val RAW_BBCODE = Stored(booleanPreferencesKey("raw_bbcode")) { it.rawBBCode }
        val SIGNED_IN_WEB_PAGES =
            Stored(booleanPreferencesKey("signed_in_web_pages")) { it.signedInWebPages }
        val DOWNLOAD_WIFI_ONLY =
            Stored(booleanPreferencesKey("download_wifi_only")) { it.downloadWifiOnly }
        val DOWNLOAD_SPEED_LIMIT =
            Stored(intPreferencesKey("download_speed_limit")) { it.downloadSpeedLimit }
        val DOWNLOAD_CONNECTIONS =
            Stored(intPreferencesKey("download_connections")) { it.downloadConnections }
        val DOWNLOAD_FILE_WRITES =
            Stored(intPreferencesKey("download_file_writes")) { it.downloadFileWrites }
        val DOWNLOAD_DECOMPRESS_THREADS =
            Stored(intPreferencesKey("download_decompress_threads")) {
                it.downloadDecompressThreads
            }
        val DOWNLOAD_LOCATION =
            Stored(stringPreferencesKey("download_location")) { it.downloadLocation }

        val REPORTED_DEVICE_TYPE =
            Stored(intPreferencesKey("reported_device_type")) { it.reportedDeviceType }

        /** Every setting, for [update] to go through. */
        val ALL = listOf(
            DARK_THEME, DYNAMIC_COLOR, MAX_SERVERS, SERVER_TIMEOUT_MS, WORKSHOP_PAGE_SIZE,
            NETWORK_LOG_SIZE, NETWORK_LOG_BODIES, RECENT_CHATS_DAYS, APPEND_NICKNAMES,
            SHOW_BLOCKED_FRIENDS, RAW_BBCODE, SIGNED_IN_WEB_PAGES, DOWNLOAD_WIFI_ONLY,
            DOWNLOAD_SPEED_LIMIT, DOWNLOAD_CONNECTIONS, DOWNLOAD_FILE_WRITES,
            DOWNLOAD_DECOMPRESS_THREADS, DOWNLOAD_LOCATION, REPORTED_DEVICE_TYPE,
        )
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val prefs = this
        val defaults = AppSettings()
        return AppSettings(
            darkTheme = DARK_THEME.read(prefs, defaults),
            dynamicColor = DYNAMIC_COLOR.read(prefs, defaults),
            maxServers = MAX_SERVERS.read(prefs, defaults),
            serverTimeoutMs = SERVER_TIMEOUT_MS.read(prefs, defaults),
            workshopPageSize = WORKSHOP_PAGE_SIZE.read(prefs, defaults),
            networkLogSize = NETWORK_LOG_SIZE.read(prefs, defaults),
            networkLogBodies = NETWORK_LOG_BODIES.read(prefs, defaults),
            recentChatsDays = RECENT_CHATS_DAYS.read(prefs, defaults),
            appendNicknames = APPEND_NICKNAMES.read(prefs, defaults),
            showBlockedFriends = SHOW_BLOCKED_FRIENDS.read(prefs, defaults),
            rawBBCode = RAW_BBCODE.read(prefs, defaults),
            signedInWebPages = SIGNED_IN_WEB_PAGES.read(prefs, defaults),
            downloadWifiOnly = DOWNLOAD_WIFI_ONLY.read(prefs, defaults),
            downloadSpeedLimit = DOWNLOAD_SPEED_LIMIT.read(prefs, defaults),
            downloadConnections = DOWNLOAD_CONNECTIONS.read(prefs, defaults),
            downloadFileWrites = DOWNLOAD_FILE_WRITES.read(prefs, defaults),
            downloadDecompressThreads = DOWNLOAD_DECOMPRESS_THREADS.read(prefs, defaults),
            downloadLocation = DOWNLOAD_LOCATION.read(prefs, defaults),
            reportedDeviceType = REPORTED_DEVICE_TYPE.read(prefs, defaults),
        )
    }

    /**
     * Changes settings: [change] is given the stored settings and returns them as they should
     * be, usually a `copy` with one value replaced. Only the values it changed are written, so
     * the rest keep following the defaults.
     */
    suspend fun update(change: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val old = prefs.toSettings()
            val new = change(old)
            ALL.forEach { it.write(prefs, old, new) }
        }
    }

    /** Clears every stored value so [settings] falls back to [AppSettings] defaults. */
    suspend fun resetToDefaults() {
        Timber.d("Resetting settings to defaults")
        context.dataStore.edit { it.clear() }
    }

    /** One setting: the key it is stored under and where it sits in [AppSettings]. */
    private class Stored<T>(val key: Preferences.Key<T>, val value: (AppSettings) -> T) {
        fun read(prefs: Preferences, defaults: AppSettings): T = prefs[key] ?: value(defaults)

        /** Stores the value in [new] if it is not the one in [old]. */
        fun write(prefs: MutablePreferences, old: AppSettings, new: AppSettings) {
            if (value(old) != value(new)) {
                Timber.d("Setting ${key.name} = ${value(new)}")
                prefs[key] = value(new)
            }
        }
    }
}
