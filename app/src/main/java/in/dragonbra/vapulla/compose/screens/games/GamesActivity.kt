package `in`.dragonbra.vapulla.compose.screens.games

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.parcelableArrayList

class GamesActivity : VapullaBaseActivity() {

    companion object {
        const val INTENT_GAMES = "intent_games"
    }

    private val viewModel: GamesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items: List<Games> = intent.extras?.parcelableArrayList(INTENT_GAMES)!!
        val name = intent.extras?.getString("name")!!
        viewModel.setContents(name, items)

        setContent {
            VapullaTheme {
                GamesScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onItemClick = { gotoGameStore(it) }
                )
            }
        }
    }

    private fun gotoGameStore(appid: Int) {
        val url = String.format(Utils.STORE_PAGE_URL, appid)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }
}
