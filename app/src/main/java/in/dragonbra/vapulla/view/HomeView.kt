package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import com.hannesdorfmann.mosby3.mvp.MvpView

interface HomeView : MvpView{
    fun closeApp()
    fun showFriends(list: List<SteamFriend>?)
    fun showAccount(account: AccountManager)
}