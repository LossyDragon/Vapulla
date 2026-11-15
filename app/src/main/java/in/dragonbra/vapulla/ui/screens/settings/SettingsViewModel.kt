package `in`.dragonbra.vapulla.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.InstallLocation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes [AppSettings] and forwards each change to [SettingsManager]. */
class SettingsViewModel(private val manager: SettingsManager, downloads: DownloadManager) :
    ViewModel() {

    val settings: StateFlow<AppSettings> = manager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Where downloads can be installed; listed anew when a folder of the user's is picked. */
    val installLocations: StateFlow<ImmutableList<InstallLocation>> =
        manager.settings
            .map { it.downloadLocation }
            .distinctUntilChanged()
            .map { picked -> downloads.installLocations(picked).toImmutableList() }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun setDarkTheme(enabled: Boolean) = save { manager.setDarkTheme(enabled) }

    fun setDynamicColor(enabled: Boolean) = save { manager.setDynamicColor(enabled) }

    fun setMaxServers(count: Int) = save { manager.setMaxServers(count) }

    fun setServerTimeoutMs(timeout: Int) = save { manager.setServerTimeoutMs(timeout) }

    fun setWorkshopPageSize(size: Int) = save { manager.setWorkshopPageSize(size) }

    fun setNetworkLogSize(size: Int) = save { manager.setNetworkLogSize(size) }
    fun setNetworkLogBodies(enabled: Boolean) = save { manager.setNetworkLogBodies(enabled) }
    fun setRecentChatsDays(days: Int) = save { manager.setRecentChatsDays(days) }
    fun setAppendNicknames(enabled: Boolean) = save { manager.setAppendNicknames(enabled) }
    fun setShowBlockedFriends(enabled: Boolean) = save { manager.setShowBlockedFriends(enabled) }
    fun setRawBBCode(enabled: Boolean) = save { manager.setRawBBCode(enabled) }
    fun setSignedInWebPages(enabled: Boolean) = save { manager.setSignedInWebPages(enabled) }
    fun setDownloadWifiOnly(enabled: Boolean) = save { manager.setDownloadWifiOnly(enabled) }
    fun setDownloadSpeedLimit(limit: Int) = save { manager.setDownloadSpeedLimit(limit) }
    fun setDownloadConnections(count: Int) = save { manager.setDownloadConnections(count) }
    fun setDownloadFileWrites(count: Int) = save { manager.setDownloadFileWrites(count) }
    fun setDownloadDecompressThreads(count: Int) =
        save { manager.setDownloadDecompressThreads(count) }
    fun setDownloadLocation(path: String) = save { manager.setDownloadLocation(path) }

    fun resetToDefaults() = save { manager.resetToDefaults() }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
