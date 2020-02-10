package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.presenter.ProfilePresenter
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.ProfileView
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import androidx.core.app.NavUtils
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_profile.*
import javax.inject.Inject

class ProfileActivity : VapullaBaseActivity<ProfileView, ProfilePresenter>(),
        ProfileView {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    @Inject
    lateinit var schemaManager: GameSchemaManager

    @Inject
    lateinit var levelManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Button setup
        profile_button_chat.click { presenter.buttonViewChat() }
        profile_button_profile.click { presenter.buttonViewProfile() }
        profile_button_games.click { presenter.buttonViewGames() }
        profile_button_manage.click { presenter.buttonViewManage() }
    }

    override fun createPresenter(): ProfilePresenter {
        val steamId = SteamID(intent.getLongExtra(ChatActivity.INTENT_STEAM_ID, 0L))

        return ProfilePresenter(
                applicationContext,
                steamId,
                steamFriendDao,
                schemaManager,
                levelManager
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> navigateUp()
        }
        return super.onOptionsItemSelected(item)
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

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }

        runOnUiThread {
            val state = EPersonaState.from(friend.state ?: 0)

            if (!friend.nickname.isNullOrEmpty()) {
                // Has nickname
                profile_name.setTypeface(null, Typeface.ITALIC)
                profile_name.text = getString(R.string.nicknameFormat, friend.nickname)
            } else {
                // No nickname
                profile_name.text = friend.name
            }

            profile_status.text =
                    Utils.getStatusText(
                            this,
                            state, friend.gameAppId,
                            friend.gameName,
                            friend.lastLogOff
                    )

            profile_icon.borderColor =
                    Utils.getStatusColor(
                            this,
                            state,
                            friend.gameAppId,
                            friend.gameName
                    )

            val flags = EPersonaStateFlag.from(friend.stateFlags)
            when {
                flags.contains(EPersonaStateFlag.ClientTypeMobile) ->
                    profile_status_indicator.setImageResource(R.drawable.ic_cellphone)
                flags.contains(EPersonaStateFlag.ClientTypeWeb) ->
                    profile_status_indicator.setImageResource(R.drawable.ic_web)
                else ->
                    profile_status_indicator.visibility = View.GONE
            }

            presenter.getLevel()
            presenter.getGameCount()

            Glide.with(this)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .apply(Utils.avatarOptions)
                    .into(profile_icon)
        }
    }

    override fun viewChat(steamId: Long) {
        startActivity(
                Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.INTENT_STEAM_ID, steamId)
                })
    }

    override fun viewProfile(url: String) {
        startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                })
    }

    override fun viewGames(url: String) {
        // TODO make own activity?
        startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                })
    }

    override fun showManageDialog(steamId: SteamID) {
        TODO("not implemented")
    }

    override fun updateBadgeLevel(level: String?) {
        Handler(mainLooper).post {
            profile_level_loading.hide()
            profile_level_count.text = level ?: "N/A"
            profile_level_count.show()
        }
    }

    override fun updateGameCount(pair: Pair<Int?, MutableList<Games>?>) {
        Handler(mainLooper).post {
            profile_games_loading.hide()
            profile_games_count.text = if (pair.first == null) "N/A" else pair.first.toString()
            profile_games_count.show()
        }
    }
}
