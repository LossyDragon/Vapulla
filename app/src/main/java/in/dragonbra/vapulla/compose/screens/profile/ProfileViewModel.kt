package `in`.dragonbra.vapulla.compose.screens.profile

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

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

    fun init() {
        val steamID = _state.value.steamID ?: return

        viewModelScope.launch {
            steamFriendDao.getFriendDetails(steamID.convertToUInt64())
                .flowOn(Dispatchers.IO)
                .onEach { friend ->
                    Timber.d("friendObserver update: $friend")

                    if (friend.relation != EFriendRelationship.Friend.code()) {
                        emit(ProfileUiEvent.NavigateBack)
                        return@onEach
                    }

                    // Load additional data in IO context
                    val profileData = withContext(Dispatchers.IO) {
                        Triple(
                            levelManager.getLevel(steamID),
                            levelManager.getGames(steamID),
                            schemaManager.touch(friend.gameAppId)
                        )
                    }

                    val (level, games, _) = profileData

                    _state.update {
                        it.copy(
                            steamID = steamID,
                            friend = friend,
                            levelCount = level,
                            gamesCount = games.count,
                            gameList = games.list,
                            isLoading = false
                        )
                    }
                }
                .catch { error ->
                    Timber.e(error, "Error loading profile data")
                    _state.update { it.copy(isLoading = false) }
                }
                .collectLatest { }  // Empty collector since we handle everything in onEach
        }
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
        val steamID = _state.value.steamID ?: return

        viewModelScope.launch {
            emit(ProfileUiEvent.SetNickName(nickName))

            steamFriendDao.find(steamID.convertToUInt64())?.let { friend ->
                steamFriendDao.update(friend.copy(nickname = nickName))
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
