package `in`.dragonbra.vapulla.view

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.adapter.GamesListItem
import `in`.dragonbra.vapulla.retrofit.response.Games
import com.hannesdorfmann.mosby3.mvp.MvpView

interface ProfileView : MvpView {
    fun closeApp()
    fun navigateUp()
    fun showAliasesDialog(nicknames: List<String>)
    fun showBlockFriendDialog(name: String)
    fun showManageDialog(steamId: SteamID)
    fun showRemoveFriendDialog(name: String)
    fun showSetNicknameDialog(nickname: String)
    fun updateBadgeLevel(level: String?)
    fun updateFriendData(friend: FriendListItem?)
    fun updateGameCount(items: GamesListItem)
    fun viewChat(steamId: Long)
    fun viewGames(gamesList: ArrayList<Games>?, friendName: String)
    fun viewProfile(url: String)
}
