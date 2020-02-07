package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.extension.click
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
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.afollestad.materialdialogs.MaterialDialog
import com.brandongogetap.stickyheaders.StickyLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.home_toolbar.*
import javax.inject.Inject

class HomeActivity : VapullaBaseActivity<HomeView, HomePresenter>(), HomeView, PopupMenu.OnMenuItemClickListener, FriendListAdapter.OnItemSelectedListener {

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

    private val updateHandler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        paperPlane = PaperPlane(this, 14.0f)
        offlineStatusUpdater = OfflineStatusUpdater(this)
        friendListAdapter = FriendListAdapter(this, gameSchemaManager, paperPlane, offlineStatusUpdater)
        friendListAdapter.listener = this

        val layoutManager = StickyLayoutManager(this, friendListAdapter)
        layoutManager.elevateHeaders(true)

        friendList.layoutManager = layoutManager
        friendList.adapter = friendListAdapter

        moreButton.click(this::openMoreMenu)
        statusButton.click(this::openStatusMenu)
        searchButton.click(this::openSearch)
        closeSearchButton.click(this::closeSearch)

        searchInput.isEnabled = false
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                presenter?.search(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateList()
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
        return true
    }

    override fun onMenuItemClick(item: MenuItem?) = when (item?.itemId) {
        R.id.logOut -> {
            presenter.disconnect()
            true
        }
        R.id.settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> false
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

    override fun showFriends(list: List<FriendListItem>, updateTime: Long) {
        friendListAdapter.swap(list, updateTime)
    }

    override fun showAccount(account: AccountManager) {
        runOnUiThread {
            localUsername.text = account.nickname
            localStatus.text = account.state.toString()

            Glide.with(this@HomeActivity)
                    .load(Utils.getAvatarUrl(account.avatarHash))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(localAvatar)
        }
    }

    override fun onItemSelected(friend: FriendListItem) {
        startActivity(Intent(this, ChatActivity::class.java).also {
            it.putExtra(ChatActivity.INTENT_STEAM_ID, friend.id)
        })
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
        if (friend.name.isNullOrBlank())
            warn("showBlockFriendDialog() name is null or blank!")

        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleBlockFriend, friend.name))
            message(text =  getString(R.string.dialogMessageBlockFriend, friend.name))
            positiveButton(R.string.dialogYes) { presenter.confirmBlockFriend(friend) }
            negativeButton(R.string.dialogNo)
        }
    }

    private fun updateList() {
        offlineStatusUpdater.updateAll()
        updateHandler.postDelayed({ updateList() }, UPDATE_INTERVAL)
    }

    private fun openMoreMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(R.menu.menu_home, popup.menu)
        popup.show()
        popup.setOnMenuItemClickListener(this)
    }

    private fun openStatusMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(R.menu.menu_status, popup.menu)
        popup.setOnMenuItemClickListener {
            val status = when (it.itemId) {
                R.id.online -> EPersonaState.Online
                R.id.away -> EPersonaState.Away
                R.id.busy -> EPersonaState.Busy
                R.id.lookingToTrade -> EPersonaState.LookingToTrade
                R.id.lookingToPlay -> EPersonaState.LookingToPlay
                else -> EPersonaState.Offline
            }
            presenter.changeStatus(status)
            true
        }
        popup.show()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun openSearch(v: View) {
        val trans = ChangeBounds()
        trans.interpolator = AnticipateOvershootInterpolator(1.0f)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.home_toolbar_frame_search)
        TransitionManager.beginDelayedTransition(toolbarLayout, trans)
        constraintSet.applyTo(toolbarLayout)
        searchInput.isEnabled = true
    }

    private fun closeSearch(v: View) {
        val trans = ChangeBounds()
        trans.interpolator = AnticipateOvershootInterpolator(1.0f)

        trans.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) {
                searchInput.setText("")
                searchInput.isEnabled = false
                Utils.hideKeyboardFrom(this@HomeActivity, v)
            }

            override fun onTransitionResume(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionCancel(transition: Transition) {
            }

            override fun onTransitionStart(transition: Transition) {
            }
        })

        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.home_toolbar)
        TransitionManager.beginDelayedTransition(toolbarLayout, trans)
        constraintSet.applyTo(toolbarLayout)
    }
}
