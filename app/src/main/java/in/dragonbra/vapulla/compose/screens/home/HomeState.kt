package `in`.dragonbra.vapulla.compose.screens.home

import androidx.lifecycle.MutableLiveData
import `in`.dragonbra.vapulla.adapter.FriendListItem

data class HomeState(
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,

    val list: MutableLiveData<List<FriendListItem>> = MutableLiveData(listOf()),
    val updateTime: Long = 0L,

    val nickname: String = "",
    val status: String = "",
    val avatarHash: String = "",

    val recentsTimeout: Int = 0,
    val sortPrefs: Int = 0
)
