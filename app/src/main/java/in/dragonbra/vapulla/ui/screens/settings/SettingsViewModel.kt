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

    /**
     * Changes settings. [change] is applied to the settings as they are stored, not as the
     * screen last saw them, so two changes in quick succession cannot undo one another.
     */
    fun update(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { manager.update(change) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { manager.resetToDefaults() }
    }
}
