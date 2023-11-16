package `in`.dragonbra.vapulla.compose.screens.profile

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.retrofit.response.Game
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed class ProfileUiEvent {
    data class SetNickName(val nickName: String) : ProfileUiEvent()
    data object BlockFriend : ProfileUiEvent()
    data object GetAliases : ProfileUiEvent()
    data object NavigateBack : ProfileUiEvent()
    data object RemoveFriend : ProfileUiEvent()
}

data class ProfileState(
    val aliasHistory: List<String> = listOf(),
    val friend: FriendListItem? = null,
    val gamesCount: Int? = 0,
    val gameList: ArrayList<Game> = arrayListOf(),
    val isLoading: Boolean = true,
    val levelCount: Int? = 0,
    val steamID: SteamID? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private var steamFriendDao: SteamFriendDao,
    private var schemaManager: GameSchemaManager,
    private var levelManager: ProfileManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private lateinit var friendData: LiveData<FriendListItem>
    private val friendObserver = Observer<FriendListItem> { friend ->
        Timber.d("friendObserver update: $friend")

        if (friend.relation != EFriendRelationship.Friend.code()) {
            // No longer a friend while viewing profile, go back.
            emit(ProfileUiEvent.NavigateBack)
            return@Observer
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val level = levelManager.getLevel(state.value.steamID!!)
                val games = levelManager.getGames(state.value.steamID!!)
                schemaManager.touch(friend.gameAppId)

                _state.update {
                    it.copy(
                        friend = friend,
                        levelCount = level,
                        gamesCount = games.count,
                        gameList = games.list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onPostCreate(owner: LifecycleOwner) {
        val steamID = state.value.steamID!!
        Timber.d("onPostCreate($owner, $steamID)")

        _state.update { it.copy(steamID = steamID) }

        friendData = steamFriendDao.findLive(steamID.convertToUInt64())
        friendData.observe(owner, friendObserver)

        _state.update { it.copy(friend = friendData.value) }
    }

    fun onDestroy() {
        friendData.removeObserver(friendObserver)
    }

    fun onAliasHistory(callback: AliasHistoryCallback) {
        val list = callback.responses[0].names.toList().sortedByDescending { it.nameSince }
        val nickNames = list.map { it.name }
        _state.update { it.copy(aliasHistory = nickNames) }
    }

    fun setSteamID(steamID: SteamID) {
        _state.update { it.copy(steamID = steamID) }
    }

    fun setNickname(nickName: String) {
        if (_state.value.steamID == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            emit(ProfileUiEvent.SetNickName(nickName))

            steamFriendDao.find(_state.value.steamID!!.convertToUInt64())?.let { friend ->
                friend.nickname = nickName
                steamFriendDao.update(friend)
            }
        }
    }

    fun removeFriend() {
        emit(ProfileUiEvent.RemoveFriend)
    }

    fun blockFriend() {
        emit(ProfileUiEvent.BlockFriend)
    }

    fun getAlias() {
        emit(ProfileUiEvent.GetAliases)
    }

    private fun emit(event: ProfileUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }
}
