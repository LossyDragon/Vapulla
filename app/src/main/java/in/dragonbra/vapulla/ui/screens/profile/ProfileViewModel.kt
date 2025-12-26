package `in`.dragonbra.vapulla.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.ProfileInfo
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    friendDao: SteamFriendDao,
    private val serviceConnection: ServiceConnection,
    friendId: Long,
) : ViewModel() {

    val friend: StateFlow<SteamFriend?> = friendDao.findFlow(friendId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _friendProfile = MutableStateFlow<ProfileInfo?>(null)
    val friendProfile: StateFlow<ProfileInfo?> = _friendProfile.asStateFlow()

    init {
        viewModelScope.launch {
            val friendId = friendId.toSteamID()
            val response = serviceConnection.steamService!!.getProfileInfo(friendId)
            if (response.result == EResult.OK) {
                _friendProfile.value = ProfileInfo.deserialize(response)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }

    fun onBlock(friendId: Long) {
        viewModelScope.launch {
            serviceConnection.steamService!!.ignoreFriend(friendId)
        }
    }

    fun onRemove(friendId: Long) {
        viewModelScope.launch {
            serviceConnection.steamService!!.removeFriend(friendId)
        }
    }

    fun onNickName(value: String) {
        viewModelScope.launch {
            serviceConnection.steamService!!.setNickName(friend.value!!.id, value)
        }
    }

    fun onAlias() {
        viewModelScope.launch {
            serviceConnection.steamService!!.requestAliasHistory(friend.value!!.id)
        }
    }
}
