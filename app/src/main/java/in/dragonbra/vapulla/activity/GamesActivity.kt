package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.GamesAdapter
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
import kotlinx.android.synthetic.main.activity_games.*

class GamesActivity : VapullaBaseActivity<GamesView, GamesPresenter>(),
        GamesView,
        GamesAdapter.OnItemSelectedListener,
        SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {

    companion object {
        const val INTENT_GAMES = "intent_games"
    }

    private lateinit var gamesAdapter: GamesAdapter

    private lateinit var items: MutableList<Games>

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_games)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        gamesAdapter = GamesAdapter(this)
        gamesAdapter.listener = this

        val layoutManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(gamesList.context, layoutManager.orientation)

        gamesList.layoutManager = layoutManager
        gamesList.adapter = gamesAdapter
        gamesList.addItemDecoration(divider)
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

        menu.findItem(R.id.search).setOnActionExpandListener(this)
        searchView.setOnQueryTextListener(this)
        searchView.queryHint = getString(R.string.gamesListSearchViewHint)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigateUp()
                true
            }
            R.id.menuSortNames -> {
                presenter.showList(GamesAdapter.SORT_ALPHABETICAL)
                true
            }
            R.id.menuSortPlaytime -> {
                presenter.showList(GamesAdapter.SORT_PLAYTIME)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun closeApp() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            finish()
        }
    }

    override fun navigateUp() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun updateGames(list: MutableList<Games>, direction: Int) {
        gamesAdapter.setList(list, direction)
        println("AAA")
    }

    override fun onMoreItemSelected(game: Games) {
        val url = String.format(Utils.STORE_PAGE_URL, game.appid)
        startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                })
    }

    override fun onQueryTextSubmit(query: String?): Boolean = true

    override fun onQueryTextChange(newText: String?): Boolean {
        presenter.search(newText!!)
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        presenter.showList()
        return true
    }
}
