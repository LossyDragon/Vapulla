package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.view.GamesView
import android.content.Context

class GamesPresenter(context: Context,
                     private val list: MutableList<Games>
) : VapullaPresenter<GamesView>(context) {

    private var sort = 0

    fun showList(direction: Int? = null) {
        if (direction != null) sort = direction
        ifViewAttached {
            it.updateGames(list, sort)
        }
    }

    fun search(query: String) {
        val trimmedQuery = query.trim()

        list.let { it ->
            if (Strings.isNullOrEmpty(trimmedQuery)) {
                ifViewAttached {
                    it.updateGames(list, sort)
                }
                return@let
            }

            val filtered = it.filter {
                it.name.contains(trimmedQuery, true)
            }.toMutableList()

            ifViewAttached {
                it.updateGames(filtered, sort)
            }
        }
    }
}
