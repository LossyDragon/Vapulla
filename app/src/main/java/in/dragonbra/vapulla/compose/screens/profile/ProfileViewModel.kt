package `in`.dragonbra.vapulla.compose.screens.profile

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.JobID
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private var jobID = JobID.INVALID

    private val profileExec by lazy {
        viewModelScope.launch(Dispatchers.IO) {
            val level = levelManager.getLevel(state.value.steamID!!)
            val games = levelManager.getGames(state.value.steamID!!)
            _state.update {
                it.copy(
                    levelCount = level,
                    gamesCount = games.count,
                    gamesList = games.list,
                    isLoading = false
                )
            }
        }
    }

    private lateinit var friendData: LiveData<FriendListItem>
    private val friendObserver = Observer<FriendListItem> { friend ->
        if (friend.relation == EFriendRelationship.Friend.code()) {
            _state.update { it.copy(friend = friend) }
            state.value.steamID?.let { profileExec }
            return@Observer
        }

        emit(ProfileUiEvent.NavigateBack)
    }

    fun onPostCreate(owner: LifecycleOwner, steamID: SteamID) {
        _state.update { it.copy(steamID = steamID) }

        friendData = steamFriendDao.findLive(steamID.convertToUInt64())
        friendData.observe(owner, friendObserver)

        friendData.value?.let {
            if (it.gameAppId > 0) schemaManager.touch(it.gameAppId)
        }

        _state.update { it.copy(friend = friendData.value) }
    }

    fun onDestroy() {
        friendData.removeObserver(friendObserver)
    }

    fun setJobID(jobID: JobID?) {
        this.jobID = jobID ?: JobID.INVALID
    }

    fun onAliasHistory(callback: AliasHistoryCallback) {
        if (jobID == callback.jobID) {
            val list = callback.responses[0].names.toList().sortedByDescending { it.nameSince }
            val nickNames = list.map { it.name }
            _state.update { it.copy(aliasHistory = nickNames) }
        }
    }

    fun setNickname(nickName: String) {
        if (_state.value.steamID == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            emit(ProfileUiEvent.SetNickName(_state.value.steamID!!, nickName))

            val friend = steamFriendDao.find(_state.value.steamID!!.convertToUInt64())
            if (friend != null) {
                friend.nickname = nickName
                steamFriendDao.update(friend)
            }
        }
    }

    fun removeFriend() {
        emit(ProfileUiEvent.RemoveFriend(_state.value.steamID!!))
    }

    fun blockFriend() {
        emit(ProfileUiEvent.BlockFriend(_state.value.steamID!!))
    }

    fun getAlias() {
        emit(ProfileUiEvent.GetAliases(_state.value.steamID!!))
    }

    private fun emit(event: ProfileUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }
}
