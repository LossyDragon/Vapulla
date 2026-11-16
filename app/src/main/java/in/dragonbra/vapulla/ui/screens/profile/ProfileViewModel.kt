package `in`.dragonbra.vapulla.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.data.ProfileInfo
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.EquippedProfileItems
import `in`.dragonbra.vapulla.service.api.FavoriteBadge
import `in`.dragonbra.vapulla.service.api.MobileNotifications
import `in`.dragonbra.vapulla.service.api.SteamFavorites
import `in`.dragonbra.vapulla.service.api.SteamFriendPreferences
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import `in`.dragonbra.vapulla.util.helpers.toSteamIdString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Profile screen state for one friend: the stored row plus the profile Steam returns. A
 * request that fails is logged and leaves the screen as it was.
 */
class ProfileViewModel(
    private val friendDao: SteamFriendDao,
    private val connection: SteamServiceConnection,
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

    /** True until Steam has answered for the badge; a friend may have none to show. */
    private val _badgeLoading = MutableStateFlow(true)
    val badgeLoading: StateFlow<Boolean> = _badgeLoading.asStateFlow()

    private val _equipped = MutableStateFlow(EquippedProfileItems())

    /** The frame, animated avatar and backgrounds on the profile; all null until Steam answers. */
    val equipped: StateFlow<EquippedProfileItems> = _equipped.asStateFlow()

    private val _mutualFriends = MutableStateFlow<ImmutableList<String>?>(null)

    /**
     * For someone who sent a friend request: the names of the friends they share with the
     * account, empty when there are none. Null for everyone else, and until Steam answers.
     */
    val mutualFriends: StateFlow<ImmutableList<String>?> = _mutualFriends.asStateFlow()

    init {
        viewModelScope.launch {
            connection
                .request("GetProfileItemsEquipped") {
                    it.profile.equippedItems(friendId.toSteamID())
                }
                .getOrNull()
                ?.let { _equipped.value = it }
        }

        viewModelScope.launch {
            connection.request("GetProfileInfo") { it.friends.profileInfo(friendId.toSteamID()) }
                .onSuccess { response ->
                    if (response.result == EResult.OK) {
                        _friendProfile.value = ProfileInfo.from(response)
                    }
                }
        }

        viewModelScope.launch {
            _favoriteBadge.value = connection
                .request("GetFavoriteBadge") { it.profile.favoriteBadge(friendId.toSteamID()) }
                .getOrNull()
            _badgeLoading.value = false
        }

        viewModelScope.launch {
            if (friendDao.find(friendId)?.isRequestRecipient != true) return@launch

            val mutual = connection.request("GetMutualFriendsForIncomingInvites") {
                it.friends.mutualFriendsOfRequests()[friendId].orEmpty()
            }.getOrNull() ?: return@launch

            _mutualFriends.value = mutual
                .map { id -> friendDao.find(id)?.nameOrNickname ?: id.toSteamIdString() }
                .sorted()
                .toImmutableList()
        }
    }

    /** Whether the friend is one of the account's favourites on Steam. */
    val isFavorite: StateFlow<Boolean> = SteamFavorites.friendIds
        .map { friendId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Makes the friend a favourite, or no longer one; [isFavorite] follows Steam's answer. */
    fun toggleFavorite() {
        viewModelScope.launch {
            val favorite = !isFavorite.value
            connection.request("SetFavorites") { it.favorites.setFavorite(friendId, favorite) }
        }
    }

    /** Whether Steam is set to notify of this friend's chat messages on a phone. */
    val mobileNotifications: StateFlow<MobileNotifications> = SteamFriendPreferences.mobile
        .map { it[friendId] ?: MobileNotifications.Default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MobileNotifications.Default)

    /** Changes that setting on Steam; [mobileNotifications] follows Steam's answer. */
    fun setMobileNotifications(setting: MobileNotifications) {
        viewModelScope.launch {
            connection.request("SetPerFriendPreferences") {
                it.friendPreferences.setMobileNotifications(friendId, setting)
            }
        }
    }

    /** Blocks the friend, or unblocks one who is blocked. */
    fun toggleBlocked() {
        viewModelScope.launch {
            connection.request("IgnoreFriend") { it.friends.toggleBlocked(friendId) }
        }
    }

    fun removeFriend() {
        viewModelScope.launch {
            connection.request("RemoveFriend") { it.friends.removeFriend(friendId) }
        }
    }

    fun setNickname(nickname: String) {
        viewModelScope.launch {
            connection.request("SetNickname") { it.friends.setNickname(friendId, nickname) }
        }
    }

    /** Asks Steam for the friend's earlier names, which then arrive in [friend]. */
    fun loadAliases() {
        viewModelScope.launch {
            connection.request("AliasHistory") { it.friends.loadAliases(friendId) }
        }
    }
}
