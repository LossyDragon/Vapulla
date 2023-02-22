package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.GamesAdapter
import `in`.dragonbra.vapulla.databinding.ActivityGamesBinding
import `in`.dragonbra.vapulla.extension.setOnQueryTextListener
import `in`.dragonbra.vapulla.presenter.GamesPresenter
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.GamesView
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamesActivity :
    VapullaBaseActivity<GamesView, GamesPresenter>(),
    GamesView {

    companion object {
        const val INTENT_GAMES = "intent_games"
    }

    private lateinit var gamesAdapter: GamesAdapter

    private lateinit var items: MutableList<Games>

    private lateinit var binding: ActivityGamesBinding

    private val menuActions = object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
            presenter.showList()
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGamesBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        gamesAdapter = GamesAdapter()
        gamesAdapter.onOverflow = {
            val url = String.format(Utils.STORE_PAGE_URL, it.appid)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(binding.gamesList.context, layoutManager.orientation)

        binding.gamesList.layoutManager = layoutManager
        binding.gamesList.adapter = gamesAdapter
        binding.gamesList.addItemDecoration(divider)
    }

    override fun onResume() {
        super.onResume()
        presenter.showList()
    }

    override fun createPresenter(): GamesPresenter {
        val bundle = intent.extras
        items = bundle?.getParcelableArrayList(INTENT_GAMES)!!

        // Set the title text since we're getting the data here anyways.
        supportActionBar?.title = bundle.getString("name") + getString(R.string.textGamesTitle)

        return GamesPresenter(applicationContext, items)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        menuInflater.inflate(R.menu.menu_games, menu)

        val searchView = menu?.findItem(R.id.search)?.actionView as SearchView

        menu.findItem(R.id.search).setOnActionExpandListener(menuActions)
        searchView.setOnQueryTextListener(
            onQueryTextSubmit = {
                true
            },
            onQueryTextChange = {
                presenter.search(it!!)
                true
            }
        )
        searchView.queryHint = getString(R.string.gamesListSearchViewHint)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> navigateUp()
            R.id.menuSortNames -> presenter.showList(GamesAdapter.SORT_ALPHABETICAL)
            R.id.menuSortPlaytime -> presenter.showList(GamesAdapter.SORT_PLAYTIME)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun closeApp() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
        finish()
    }

    override fun navigateUp() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun updateGames(list: MutableList<Games>, direction: Int) {
        gamesAdapter.setList(list, direction)
    }
}
