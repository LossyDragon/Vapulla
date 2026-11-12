package `in`.dragonbra.vapulla.manager

import android.content.Context
import androidx.compose.runtime.Immutable
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
) {
    companion object {
        val MAX_SERVERS_RANGE = 25..500
        const val MAX_SERVERS_STEP = 25
        val SERVER_TIMEOUT_RANGE = 500..5000
        const val SERVER_TIMEOUT_STEP = 250
        val WORKSHOP_PAGE_SIZES = listOf(10, 20, 50, 100)
        val NETWORK_LOG_SIZES = listOf(100, 250, 500, 1000)
    }
}

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
            )
        }

    suspend fun setDarkTheme(enabled: Boolean) = set(DARK_THEME, enabled)

    suspend fun setDynamicColor(enabled: Boolean) = set(DYNAMIC_COLOR, enabled)

    suspend fun setMaxServers(count: Int) = set(MAX_SERVERS, count)

    suspend fun setServerTimeoutMs(timeout: Int) = set(SERVER_TIMEOUT_MS, timeout)

    suspend fun setWorkshopPageSize(size: Int) = set(WORKSHOP_PAGE_SIZE, size)

    suspend fun setNetworkLogSize(size: Int) = set(NETWORK_LOG_SIZE, size)

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
