package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.retrofit.response.Games
import com.hannesdorfmann.mosby3.mvp.MvpView

interface GamesView : MvpView {
    fun closeApp()
    fun navigateUp()
    fun updateGames(list: MutableList<Games>, direction: Int)
}
