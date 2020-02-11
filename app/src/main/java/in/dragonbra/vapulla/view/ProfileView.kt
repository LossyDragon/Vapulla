package `in`.dragonbra.vapulla.view

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.retrofit.response.Games
import com.hannesdorfmann.mosby3.mvp.MvpView

interface ProfileView : MvpView {
    fun closeApp()
    fun navigateUp()
    fun updateFriendData(friend: FriendListItem?)
    fun viewChat(steamId: Long)
    fun viewProfile(url: String)
    fun viewGames(url: String)
    fun showManageDialog(steamId: SteamID)
    fun updateBadgeLevel(level: String?)
    fun updateGameCount(pair: Pair<Int?, MutableList<Games>?>)
    fun showAliasesDialog(nicknames: List<String>)
    fun showBlockFriendDialog(name: String?)
    fun showRemoveFriendDialog(name: String?)
    fun showSetNicknameDialog(nickname: String?)
}
