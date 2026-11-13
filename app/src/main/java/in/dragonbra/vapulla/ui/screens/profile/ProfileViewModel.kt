package `in`.dragonbra.vapulla.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.data.ProfileInfo
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.service.api.FavoriteBadge
import `in`.dragonbra.vapulla.service.api.FriendsApi
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Profile screen state for one friend: the stored row plus the profile Steam returns. */
class ProfileViewModel(
    friendDao: SteamFriendDao,
    private val serviceConnection: ServiceConnection,
    private val friendId: Long,
) : ViewModel() {

    val friend: StateFlow<SteamFriend?> = friendDao.findFlow(friendId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _friendProfile = MutableStateFlow<ProfileInfo?>(null)
    val friendProfile: StateFlow<ProfileInfo?> = _friendProfile.asStateFlow()

    private val _favoriteBadge = MutableStateFlow<FavoriteBadge?>(null)
    val favoriteBadge: StateFlow<FavoriteBadge?> = _favoriteBadge.asStateFlow()

    private val service: SteamService
        get() = checkNotNull(serviceConnection.steamService) { "SteamService is not bound" }

    private val friends: FriendsApi
        get() = checkNotNull(service.apis) { "Not connected to Steam" }.friends

    init {
        viewModelScope.launch {
            val response = friends.getProfileInfo(friendId.toSteamID())
            if (response.result == EResult.OK) {
                _friendProfile.value = ProfileInfo.from(response)
            }
        }
        viewModelScope.launch {
            _favoriteBadge.value =
                runCatching { service.apis?.profile?.favoriteBadge(friendId.toSteamID()) }
                    .onFailure { Timber.w(it, "GetFavoriteBadge failed") }
                    .getOrNull()
        }
    }

    override fun onCleared() {
        Timber.d("onCleared")
    }

    fun onBlock() {
        viewModelScope.launch { friends.ignoreFriend(friendId) }
    }

    fun onRemove() {
        viewModelScope.launch { friends.removeFriend(friendId) }
    }

    fun onNickname(value: String) {
        viewModelScope.launch { friends.setNickname(friendId, value) }
    }

    fun onAlias() {
        viewModelScope.launch { friends.requestAliasHistory(friendId) }
    }
}
