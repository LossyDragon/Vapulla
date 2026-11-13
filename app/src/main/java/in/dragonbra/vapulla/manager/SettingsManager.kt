package `in`.dragonbra.vapulla.manager

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
) {
    companion object {
        val RECENT_CHATS_DAYS = listOf(1, 5, 7, 30)
        val MAX_SERVERS_RANGE = 25..500
        const val MAX_SERVERS_STEP = 25
        val SERVER_TIMEOUT_RANGE = 500..5000
        const val SERVER_TIMEOUT_STEP = 250
        val WORKSHOP_PAGE_SIZES = listOf(10, 20, 50, 100)
        val NETWORK_LOG_SIZES = listOf(100, 250, 500, 1000)
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

        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val MAX_SERVERS = intPreferencesKey("max_servers")
        val SERVER_TIMEOUT_MS = intPreferencesKey("server_timeout_ms")
        val WORKSHOP_PAGE_SIZE = intPreferencesKey("workshop_page_size")
        val NETWORK_LOG_SIZE = intPreferencesKey("network_log_size")
        val NETWORK_LOG_BODIES = booleanPreferencesKey("network_log_bodies")
        val RECENT_CHATS_DAYS = intPreferencesKey("recent_chats_days")
        val APPEND_NICKNAMES = booleanPreferencesKey("append_nicknames")
        val SHOW_BLOCKED_FRIENDS = booleanPreferencesKey("show_blocked_friends")
        val RAW_BBCODE = booleanPreferencesKey("raw_bbcode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                darkTheme = prefs[DARK_THEME] ?: defaults.darkTheme,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: defaults.dynamicColor,
                maxServers = prefs[MAX_SERVERS] ?: defaults.maxServers,
                serverTimeoutMs = prefs[SERVER_TIMEOUT_MS] ?: defaults.serverTimeoutMs,
                workshopPageSize = prefs[WORKSHOP_PAGE_SIZE] ?: defaults.workshopPageSize,
                networkLogSize = prefs[NETWORK_LOG_SIZE] ?: defaults.networkLogSize,
                networkLogBodies = prefs[NETWORK_LOG_BODIES] ?: defaults.networkLogBodies,
                recentChatsDays = prefs[RECENT_CHATS_DAYS] ?: defaults.recentChatsDays,
                appendNicknames = prefs[APPEND_NICKNAMES] ?: defaults.appendNicknames,
                showBlockedFriends = prefs[SHOW_BLOCKED_FRIENDS] ?: defaults.showBlockedFriends,
                rawBBCode = prefs[RAW_BBCODE] ?: defaults.rawBBCode,
            )
        }

    suspend fun setDarkTheme(enabled: Boolean) = set(DARK_THEME, enabled)
    suspend fun setDynamicColor(enabled: Boolean) = set(DYNAMIC_COLOR, enabled)
    suspend fun setMaxServers(count: Int) = set(MAX_SERVERS, count)
    suspend fun setServerTimeoutMs(timeout: Int) = set(SERVER_TIMEOUT_MS, timeout)
    suspend fun setWorkshopPageSize(size: Int) = set(WORKSHOP_PAGE_SIZE, size)
    suspend fun setNetworkLogSize(size: Int) = set(NETWORK_LOG_SIZE, size)
    suspend fun setNetworkLogBodies(enabled: Boolean) = set(NETWORK_LOG_BODIES, enabled)
    suspend fun setRecentChatsDays(days: Int) = set(RECENT_CHATS_DAYS, days)
    suspend fun setAppendNicknames(enabled: Boolean) = set(APPEND_NICKNAMES, enabled)
    suspend fun setShowBlockedFriends(enabled: Boolean) = set(SHOW_BLOCKED_FRIENDS, enabled)
    suspend fun setRawBBCode(enabled: Boolean) = set(RAW_BBCODE, enabled)

    /** Clears every stored value so [settings] falls back to [AppSettings] defaults. */
    suspend fun resetToDefaults() {
        Timber.d("Resetting settings to defaults")
        context.dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        Timber.d("Setting ${key.name} = $value")
        context.dataStore.edit { it[key] = value }
    }
}
