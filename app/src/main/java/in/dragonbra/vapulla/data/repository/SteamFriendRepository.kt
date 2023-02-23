package `in`.dragonbra.vapulla.data.repository

import androidx.lifecycle.MutableLiveData
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SteamFriendRepository(private val steamFriendDao: SteamFriendDao) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val foundFriend = MutableLiveData<SteamFriend>()
    val findLiveFriends = MutableLiveData<FriendListItem>()
    val getLiveFriends = MutableLiveData<List<FriendListItem>>()

    fun insert(vararg steamFriends: SteamFriend) {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.insert(steamFriends = steamFriends)
        }
    }

    fun find(id: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            foundFriend.postValue(steamFriendDao.find(id = id))
        }
    }

    fun findLive(id: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val data = steamFriendDao.findLive(id = id)
            findLiveFriends.postValue(data.value)
        }
    }

    fun update(vararg steamFriends: SteamFriend) {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.update(steamFriends = steamFriends)
        }
    }

    fun getLive() {
        coroutineScope.launch(Dispatchers.IO) {
            val data = steamFriendDao.getLive()
            getLiveFriends.postValue(data.value)
        }
    }

    fun clearNicknames() {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.clearNicknames()
        }
    }

    fun remove(vararg friends: SteamFriend) {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.remove(friends = friends)
        }
    }

    fun delete() {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.delete()
        }
    }

    fun clearOnlineState() {
        coroutineScope.launch(Dispatchers.IO) {
            steamFriendDao.clearOnlineState()
        }
    }
}
