package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.databinding.ActivityHomeBinding
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.setOnQueryTextListener
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.warn
import `in`.dragonbra.vapulla.view.HomeView
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import com.afollestad.materialdialogs.MaterialDialog
import com.brandongogetap.stickyheaders.StickyLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity :
    VapullaBaseActivity<HomeView, HomePresenter>(),
    HomeView,
    FriendListAdapter.OnItemSelectedListener,
    MenuItem.OnActionExpandListener {

    companion object {
        const val UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS
    }

    @Inject
    lateinit var homePresenter: HomePresenter

    @Inject
    lateinit var gameSchemaManager: GameSchemaManager

    private lateinit var paperPlane: PaperPlane

    private lateinit var offlineStatusUpdater: OfflineStatusUpdater

    private lateinit var friendListAdapter: FriendListAdapter

    private val updateHandler: Handler = Handler(Looper.getMainLooper())

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        paperPlane = PaperPlane(this, 14.0f)
        offlineStatusUpdater = OfflineStatusUpdater(this)
        friendListAdapter =
            FriendListAdapter(this, gameSchemaManager, paperPlane, offlineStatusUpdater)
        friendListAdapter.listener = this

        val layoutManager = StickyLayoutManager(this, friendListAdapter)
        layoutManager.elevateHeaders(true)

        binding.friendList.layoutManager = layoutManager
        binding.friendList.adapter = friendListAdapter

        setSupportActionBar(binding.homeToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.statusButton.click(this::openStatusMenu)

        binding.friendsListSwipe.setOnRefreshListener {
            presenter.refreshFriendsList()
            binding.friendsListSwipe.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
        // presenter.refreshFriendsList()
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
        offlineStatusUpdater.clear()
    }

    override fun createPresenter(): HomePresenter = homePresenter

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        menuInflater.inflate(R.menu.menu_home, menu)

        // Expanding animation would be nice
        val searchView = menu?.findItem(R.id.search)?.actionView as SearchView

        menu.findItem(R.id.search).setOnActionExpandListener(this)
        searchView.queryHint = getString(R.string.friendsListSearchViewHint)
        searchView.setOnQueryTextListener(
            onQueryTextSubmit = {
                true
            },
            onQueryTextChange = {
                presenter.search(it!!)
                true
            }
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logOut -> presenter.disconnect()
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun closeApp() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                addCategory(Intent.CATEGORY_HOME)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun showFriends(list: List<FriendListItem>, updateTime: Long) {
        friendListAdapter.swap(list.toMutableList(), updateTime)
    }

    override fun showAccount(account: AccountManager) {
        runOnUiThread {
            binding.toolbar.localUsername.text = account.nickname
            binding.toolbar.localStatus.text = account.state.toString()

            Glide.with(this@HomeActivity)
                .load(Utils.getAvatarUrl(account.avatarHash))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(Utils.avatarOptions)
                .into(binding.toolbar.localAvatar)
        }
    }

    override fun onItemSelected(friend: FriendListItem) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.INTENT_STEAM_ID, friend.id)
        }
        startActivity(intent)
    }

    override fun onLongItemSelected(friend: FriendListItem) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra(ProfileActivity.INTENT_STEAM_ID, friend.id)
        }
        startActivity(intent)
    }

    override fun onRequestAccept(friend: FriendListItem) {
        presenter.acceptRequest(friend)
    }

    override fun onRequestIgnore(friend: FriendListItem) {
        presenter.ignoreRequest(friend)
    }

    override fun onRequestBlock(friend: FriendListItem) {
        presenter.blockRequest(friend)
    }

    override fun showBlockFriendDialog(friend: FriendListItem) {
        if (friend.name.isNullOrBlank()) {
            warn("showBlockFriendDialog() name is null or blank!")
            return
        }

        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleBlockFriend, friend.name))
            message(text = getString(R.string.dialogMessageBlockFriend, friend.name))
            positiveButton(R.string.dialogYes) { presenter.confirmBlockFriend(friend) }
            negativeButton(R.string.dialogNo)
        }
    }

    private fun updateList() {
        offlineStatusUpdater.updateAll()
        updateHandler.postDelayed({ updateList() }, UPDATE_INTERVAL)
    }

    private fun openStatusMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(R.menu.menu_status, popup.menu)
        popup.setOnMenuItemClickListener {
            val status = when (it.itemId) {
                R.id.online -> EPersonaState.Online
                R.id.away -> EPersonaState.Away
                R.id.invisible -> EPersonaState.Invisible
                R.id.offline -> EPersonaState.Offline
                else -> EPersonaState.Offline
            }
            presenter.changeStatus(status)
            true
        }
        popup.show()
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        presenter.setSearchStatus(true)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        presenter.setSearchStatus(false)
        // presenter.refreshFriendsList()
        return true
    }
}
