package `in`.dragonbra.vapulla.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.manager.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes [AppSettings] and forwards each change to [SettingsManager]. */
class SettingsViewModel(private val manager: SettingsManager) : ViewModel() {

    val settings: StateFlow<AppSettings> = manager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDarkTheme(enabled: Boolean) = save { manager.setDarkTheme(enabled) }

    fun setDynamicColor(enabled: Boolean) = save { manager.setDynamicColor(enabled) }

    fun setMaxServers(count: Int) = save { manager.setMaxServers(count) }

    fun setServerTimeoutMs(timeout: Int) = save { manager.setServerTimeoutMs(timeout) }

    fun setWorkshopPageSize(size: Int) = save { manager.setWorkshopPageSize(size) }

    fun setNetworkLogSize(size: Int) = save { manager.setNetworkLogSize(size) }
    fun setRecentChatsDays(days: Int) = save { manager.setRecentChatsDays(days) }
    fun setAppendNicknames(enabled: Boolean) = save { manager.setAppendNicknames(enabled) }
    fun setShowBlockedFriends(enabled: Boolean) = save { manager.setShowBlockedFriends(enabled) }

    fun resetToDefaults() = save { manager.resetToDefaults() }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
