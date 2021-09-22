package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.adapter.GamesListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.databinding.ActivityProfileBinding
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.presenter.ProfilePresenter
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.ProfileView
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.core.app.NavUtils
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity :
    VapullaBaseActivity<ProfileView, ProfilePresenter>(),
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

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Button setup
        binding.profileButtonChat.click { presenter.buttonViewChat() }
        binding.profileButtonProfile.click { presenter.buttonViewProfile() }
        binding.profileButtonGames.click { presenter.buttonViewGames() }
        binding.profileButtonManage.click { presenter.buttonViewManage() }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.clearGamesList()
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
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun closeApp() {
        presenter.clearGamesList()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
        finish()
    }

    override fun navigateUp() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }

        val state = EPersonaState.from(friend.state ?: 0)

        if (!friend.nickname.isNullOrEmpty()) {
            // Has nickname
            binding.profileName.setTypeface(null, Typeface.ITALIC)
            binding.profileName.text = getString(R.string.nicknameFormat, friend.nickname)
        } else {
            // No nickname
            binding.profileName.text = friend.name
        }

        binding.profileStatus.text =
            Utils.getStatusText(
                this,
                state,
                friend.gameAppId,
                friend.gameName,
                friend.lastLogOff
            )

        binding.profileIcon.borderColor =
            Utils.getStatusColor(this, state, friend.gameAppId, friend.gameName)

        val flags = EPersonaStateFlag.from(friend.stateFlags)
        when {
            flags.contains(EPersonaStateFlag.ClientTypeMobile) -> {
                binding.profileStatusIndicator.setImageResource(R.drawable.ic_cellphone)
                binding.profileStatusIndicator.show()
            }
            flags.contains(EPersonaStateFlag.ClientTypeWeb) -> {
                binding.profileStatusIndicator.setImageResource(R.drawable.ic_web)
                binding.profileStatusIndicator.show()
            }
            else -> binding.profileStatusIndicator.hide()
        }

        presenter.getLevel()
        presenter.getGameCount()

        Glide.with(this)
            .load(Utils.getAvatarUrl(friend.avatar))
            .apply(Utils.avatarOptions)
            .into(binding.profileIcon)
    }

    override fun viewChat(steamId: Long) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.INTENT_STEAM_ID, steamId)
        }
        startActivity(intent)
    }

    override fun viewProfile(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
        )
    }

    override fun viewGames(gamesList: ArrayList<Games>?, friendName: String) {
        val bundle = Bundle().apply {
            putParcelableArrayList(GamesActivity.INTENT_GAMES, gamesList)
            putString("name", friendName)
        }
        val intent = Intent(this, GamesActivity::class.java).apply {
            putExtras(bundle)
        }
        startActivity(intent)
    }

    override fun showManageDialog(steamId: SteamID) {
        PopupMenu(this, binding.profileButtonManage).apply {
            menuInflater.inflate(R.menu.menu_profile, this.menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.setNickname -> presenter.menuSetNickname()
                    R.id.viewAliases -> presenter.menuViewAliases()
                    R.id.removeFriend -> presenter.menuRemoveFriend()
                    R.id.blockFriend -> presenter.menuBlockFriend()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    override fun updateBadgeLevel(level: String?) {
        runOnUiThread {
            binding.profileLevelLoading.hide()
            binding.profileLevelCount.text = level ?: "N/A"
            binding.profileLevelCount.show()
        }
    }

    override fun updateGameCount(items: GamesListItem) {
        runOnUiThread {
            binding.profileGamesLoading.hide()
            binding.profileGamesCount.text = items.count.toString()
            binding.profileGamesCount.show()

            if (items.count == 0) {
                binding.profileButtonGames.disable()
                binding.profileButtonGames.text = getString(R.string.textNoNames)
            } else {
                binding.profileButtonGames.enable()
            }
        }

        presenter.setGamesList(items.list)
    }

    @SuppressLint("CheckResult")
    override fun showAliasesDialog(nicknames: List<String>) {
        runOnUiThread {
            MaterialDialog(this).show {
                title(R.string.dialogTitleAliases)
                listItems(items = nicknames)
                positiveButton(R.string.dialogClose)
            }
        }
    }

    override fun showBlockFriendDialog(name: String) {
        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleBlockFriend, name))
            message(text = getString(R.string.dialogMessageBlockFriend, name))
            positiveButton(R.string.dialogYes) {
                presenter.menuConfirmBlockFriend()
            }
            negativeButton(R.string.dialogNo)
        }
    }

    override fun showRemoveFriendDialog(name: String) {
        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleRemoveFriend, name))
            message(text = getString(R.string.dialogMessageRemoveFriend, name))
            positiveButton(R.string.dialogYes) {
                presenter.menuConfirmRemoveFriend()
            }
            negativeButton(R.string.dialogNo)
        }
    }

    @SuppressLint("CheckResult")
    override fun showSetNicknameDialog(nickname: String) {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.dialogTitleNickname)
            input(hint = nickname, waitForPositiveButton = true) { _, text ->
                presenter.menuConfirmSetNickName(text.toString())
            }
            positiveButton(R.string.dialogSet)
            negativeButton(R.string.dialogCancel)
        }
    }
}
