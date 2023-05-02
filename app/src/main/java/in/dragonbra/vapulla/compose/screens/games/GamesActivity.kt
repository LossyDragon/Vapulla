package `in`.dragonbra.vapulla.compose.screens.games

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.retrofit.response.Games
import timber.log.Timber

class GamesActivity : VapullaBaseActivity() {

    companion object {
        const val INTENT_GAMES = "intent_games"
    }

    private val viewModel: GamesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        val items = if (Constants.isAtLeastT) {
            intent.extras?.getParcelableArrayList(INTENT_GAMES, Games::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.extras?.getParcelableArrayList(INTENT_GAMES) ?: arrayListOf()
        }

        val name = intent.extras?.getString("name")!!
        viewModel.setContents(name, items)

        setContent {
            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    GamesScreen(
                        viewModel = viewModel,
                        onItemClick = ::gotoGameStore
                    )
                }
            }
        }
    }

    private fun gotoGameStore(appid: Int) {
        val url = String.format(Constants.STORE_PAGE_URL, appid)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }
}
