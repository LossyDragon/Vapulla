package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.ChatAdapter
import `in`.dragonbra.vapulla.adapter.EmoteAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import `in`.dragonbra.vapulla.view.ChatView
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.snackbar.Snackbar
import `in`.dragonbra.vapulla.databinding.ActivityChatBinding
import `in`.dragonbra.vapulla.databinding.DialogNicknameBinding
import `in`.dragonbra.vapulla.util.browse
import `in`.dragonbra.vapulla.util.startActivity
import javax.inject.Inject

class ChatActivity : VapullaBaseActivity<ChatView, ChatPresenter>(), ChatView, TextWatcher,
    PopupMenu.OnMenuItemClickListener, EmoteAdapter.EmoteListener {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"

        const val REQUEST_IMAGE_GET = 100
    }

    @Inject
    lateinit var chatMessageDao: ChatMessageDao

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    @Inject
    lateinit var emoticonDao: EmoticonDao

    @Inject
    lateinit var imgurAuthService: ImgurAuthService

    @Inject
    lateinit var schemaManager: GameSchemaManager

    @Inject
    lateinit var clipboard: ClipboardManager

    private lateinit var paperPlane: PaperPlane

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var emoteAdapter: EmoteAdapter

    private lateinit var binding: ActivityChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        paperPlane = PaperPlane(this, 18.0f)
        chatAdapter = ChatAdapter(this, paperPlane, clipboard)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true

        binding = ActivityChatBinding.inflate(layoutInflater)

        binding.chatList.layoutManager = layoutManager
        binding.chatList.adapter = chatAdapter

        chatAdapter.registerAdapterDataObserver(
            ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                binding.chatList
            )
        )

        emoteAdapter = EmoteAdapter(this, this)

        val emoteLayoutManager = FlexboxLayoutManager(this)
        emoteLayoutManager.flexDirection = FlexDirection.ROW
        emoteLayoutManager.justifyContent = JustifyContent.CENTER

        binding.emoteList.layoutManager = emoteLayoutManager;
        binding.emoteList.adapter = emoteAdapter

        binding.messageBox.addTextChangedListener(this)
        binding.messageBox.setOnClickListener { binding.emoteList.hide() }

        binding.moreButton.setOnClickListener {
            val popup = PopupMenu(this@ChatActivity, it)
            popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this@ChatActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
    }

    override fun createPresenter(): ChatPresenter {
        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        return ChatPresenter(
            this,
            chatMessageDao,
            steamFriendDao,
            emoticonDao,
            imgurAuthService,
            schemaManager,
            steamId
        )
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

    override fun showChat(list: PagedList<ChatMessage>?) {
        chatAdapter.submitList(list)
    }

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }
        runOnUiThread {
            val state = EPersonaState.from(friend.state ?: 0)
            binding.friendUsername.text = friend.name

            if (Strings.isNullOrEmpty(friend.nickname)) {
                binding.friendNickname.hide()
            } else {
                binding.friendNickname.show()
                binding.friendNickname.text = getString(R.string.nicknameFormat, friend.nickname)
            }

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                && friend.typingTs > System.currentTimeMillis() - 20000L
            ) {
                binding.friendStatus.text = getString(R.string.statusTyping)
                val color = ContextCompat.getColor(this@ChatActivity, R.color.colorAccent)
                binding.friendStatus.setTextColor(color)
                binding.friendStatus.bold()
            } else {
                binding.friendStatus.text = Utils.getStatusText(
                    this@ChatActivity,
                    state,
                    friend.gameAppId,
                    friend.gameName,
                    friend.lastLogOff
                )
                val color =
                    ContextCompat.getColor(this@ChatActivity, android.R.color.secondary_text_dark)
                binding.friendStatus.setTextColor(color)

                binding.friendStatus.normal()
            }

            Glide.with(this@ChatActivity)
                .load(Utils.getAvatarURL(friend.avatar))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(Utils.avatarOptions)
                .into(binding.friendAvatar)
        }
    }

    override fun navigateUp() {
        Utils.hideKeyboardFrom(this, binding.messageBox)
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.isEmpty()) {
            binding.imageButton.show()
        } else {
            binding.imageButton.hide()
        }
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.removeFriend -> {
            presenter.removeFriend()
            true
        }

        R.id.blockFriend -> {
            presenter.blockFriend()
            true
        }

        R.id.setNickname -> {
            presenter.nicknameMenuClicked()
            true
        }

        R.id.viewAccount -> {
            presenter.viewAccountMenuClicked()
            true
        }

        R.id.viewAliases -> {
            presenter.viewAliasesMenuClicked()
            true
        }

        else -> false
    }

    override fun showRemoveFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(getString(R.string.dialogMessageRemoveFriend, name))
            .setTitle(getString(R.string.dialogTitleRemoveFriend, name))
            .setPositiveButton(R.string.dialogYes, { _, _ -> presenter.confirmRemoveFriend() })
            .setNegativeButton(R.string.dialogNo, null)

        builder.create().show()
    }

    override fun showBlockFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(getString(R.string.dialogMessageBlockFriend, name))
            .setTitle(getString(R.string.dialogTitleBlockFriend, name))
            .setPositiveButton(R.string.dialogYes, { _, _ -> presenter.confirmBlockFriend() })
            .setNegativeButton(R.string.dialogNo, null)

        builder.create().show()
    }

    override fun showNicknameDialog(nickname: String) {
        val v = DialogNicknameBinding.inflate(LayoutInflater.from(this))
        v.nickname.setText(nickname)

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.dialogTitleNickname)
            .setView(v.root)
            .setPositiveButton(
                R.string.dialogSet,
                { _, _ -> presenter.setNickname(v.nickname.text.toString()) })
            .setNegativeButton(R.string.dialogCancel, null)

        builder.create().show()
    }

    override fun browseUrl(url: String) {
        browse(url)
    }

    override fun showAliases(names: List<String>) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.dialogTitleAliases)
                .setItems(names.toTypedArray(), null)
                .setNegativeButton(R.string.dialogClose, null)

            builder.create().show()
        }
    }

    override fun showEmotes(list: List<Emoticon>) {
        emoteAdapter.swap(list)
    }

    override fun onEmoteSelected(emoticon: Emoticon) {
        binding.messageBox.text.insert(binding.messageBox.selectionStart, ":${emoticon.name}:")
    }

    override fun showImgurDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(R.string.dialogMessageImgur)
            .setTitle(R.string.dialogTitleImgur)
            .setPositiveButton(R.string.dialogYes, { _, _ -> startActivity<SettingsActivity>() })
            .setNegativeButton(R.string.dialogCancel, null)

        builder.create().show()
    }

    override fun showPhotoSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            presenter.sendImage(data!!.data!!)
        }
    }

    override fun showUploadDialog() {
        runOnUiThread {
            binding.imageButton.isClickable = false
            binding.uploadProgressBar.show()
            binding.uploadProgressBar.isIndeterminate = true
        }
    }

    override fun imageUploadFail() {
        runOnUiThread {
            binding.imageButton.isClickable = true
            binding.uploadProgressBar.hide()
            Snackbar.make(binding.rootLayout, R.string.snackbarImgurUploadFailed, Snackbar.LENGTH_LONG)
                .show()
        }
    }

    override fun imageUploadSuccess() {
        runOnUiThread {
            binding.imageButton.isClickable = true
            binding.uploadProgressBar.hide()
        }
    }

    override fun imageUploadProgress(total: Int, progress: Int) {
        binding.uploadProgressBar.max = total
        binding.uploadProgressBar.progress = progress
        binding.uploadProgressBar.isIndeterminate = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigateUp(v: View) {
        navigateUp()
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendMessage(v: View) {
        val message = binding.messageBox.text.toString()

        if (!Strings.isNullOrEmpty(message)) {
            binding.messageBox.setText("")
            presenter.sendMessage(message)
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleEmote(v: View) {
        binding.emoteList.toggleVisibility()

        if (binding.emoteList.isVisible()) {
            Utils.hideKeyboardFrom(this, binding.messageBox)
            presenter.requestEmotes()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendImage(v: View) {
        presenter.imageButtonClicked()
    }
}
