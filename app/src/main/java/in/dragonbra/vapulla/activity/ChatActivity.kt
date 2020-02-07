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
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_chat.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        paperPlane = PaperPlane(this, 18.0f)
        chatAdapter = ChatAdapter(this, paperPlane, clipboard)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        chatList.layoutManager = layoutManager
        chatList.adapter = chatAdapter

        chatAdapter.registerAdapterDataObserver(ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                chatList
        ))

        emoteAdapter = EmoteAdapter(this, this)

        val emoteLayoutManager = FlexboxLayoutManager(this)
        emoteLayoutManager.flexDirection = FlexDirection.ROW
        emoteLayoutManager.justifyContent = JustifyContent.CENTER

        emoteList.layoutManager = emoteLayoutManager
        emoteList.adapter = emoteAdapter

        messageBox.addTextChangedListener(this)
        messageBox.requestFocus()
        messageBox.setOnClickListener { emoteList.hide() }

        moreButton.setOnClickListener {
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
        return ChatPresenter(this, chatMessageDao, steamFriendDao, emoticonDao, imgurAuthService, schemaManager, steamId)
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
            friendUsername.text = friend.name

            if (Strings.isNullOrEmpty(friend.nickname)) {
                friendNickname.hide()
            } else {
                friendNickname.show()
                friendNickname.text = getString(R.string.nicknameFormat, friend.nickname)
            }

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                    && friend.typingTs > System.currentTimeMillis() - 15000L) {
                friendStatus.text = getString(R.string.statusTyping)
                friendStatus.setTextColor(ContextCompat.getColor(this@ChatActivity, R.color.colorAccent))
                friendStatus.bold()
            } else {
                friendStatus.text = Utils.getStatusText(this@ChatActivity, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                friendStatus.setTextColor(ContextCompat.getColor(this@ChatActivity, R.color.colorTyping))
                friendStatus.normal()
            }

            Glide.with(this@ChatActivity)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(friendAvatar)
        }
    }

    override fun navigateUp() {
        Utils.hideKeyboardFrom(this, messageBox)
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.isEmpty()) {
            imageButton.show()
        } else {
            imageButton.hide()
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
        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleRemoveFriend, name))
            message(text = getString(R.string.dialogMessageRemoveFriend, name))
            positiveButton(R.string.dialogYes) { presenter.confirmRemoveFriend() }
            negativeButton(R.string.dialogNo)
        }
    }

    override fun showBlockFriendDialog(name: String) {
        MaterialDialog(this).show {
            title(text = getString(R.string.dialogTitleBlockFriend, name))
            message(text = getString(R.string.dialogMessageBlockFriend, name))
            positiveButton(R.string.dialogYes) { presenter.confirmBlockFriend() }
            negativeButton(R.string.dialogNo)
        }
    }

    @SuppressLint("InflateParams")
    override fun showNicknameDialog(nickname: String) {
        MaterialDialog(this).show {
            title(R.string.dialogTitleNickname)
            input(hint = nickname, waitForPositiveButton = true) { _, text ->
                presenter.setNickname(text.toString())
            }
            positiveButton(R.string.dialogSet)
            negativeButton(R.string.dialogCancel)
        }

    }

    override fun browseUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    override fun showAliases(names: List<String>) {
        runOnUiThread {
            MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                title(R.string.dialogTitleAliases)
                listItems(items = names)
                positiveButton(R.string.dialogClose)
            }
        }
    }

    override fun showEmotes(list: List<Emoticon>) {
        emoteAdapter.swap(list)
    }

    override fun onEmoteSelected(emoticon: Emoticon) {

        if(emoticon.isSticker) {
            val stickerText = "/sticker ${emoticon.name}"
            presenter.sendMessage(stickerText)
        } else {
            messageBox.text.insert(messageBox.selectionStart, ":${emoticon.name}:")
        }
    }

    override fun showImgurDialog() {
        MaterialDialog(this).show {
            title(R.string.dialogTitleImgur)
            message(R.string.dialogMessageImgur)
            positiveButton(R.string.dialogYes) {
                Intent(this@ChatActivity, SettingsActivity::class.java)
            }
            negativeButton(R.string.dialogCancel)
        }
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
            presenter.sendImage(data?.data!!)
        }
    }

    override fun showUploadDialog() {
        runOnUiThread {
            imageButton.isClickable = false
            uploadProgressBar.show()
            uploadProgressBar.isIndeterminate = true
        }
    }

    override fun imageUploadFail() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
            Snackbar.make(rootLayout, R.string.snackbarImgurUploadFailed, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun imageUploadSuccess() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
        }
    }

    override fun imageUploadProgress(total: Int, progress: Int) {
        uploadProgressBar.max = total
        uploadProgressBar.progress = progress
        uploadProgressBar.isIndeterminate = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigateUp(v: View) {
        navigateUp()
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendMessage(v: View) {
        val message = messageBox.text.toString()

        if (!Strings.isNullOrEmpty(message)) {
            messageBox.setText("")
            presenter.sendMessage(message)
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleEmote(v: View) {
        emoteList.toggleVisibility()

        if (emoteList.isVisible()) {
            Utils.hideKeyboardFrom(this, messageBoxLayout)
            presenter.requestEmotes()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendImage(v: View) {
        presenter.imageButtonClicked()
    }
}
