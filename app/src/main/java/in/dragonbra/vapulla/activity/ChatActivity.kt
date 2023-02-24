package `in`.dragonbra.vapulla.activity

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
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
import `in`.dragonbra.vapulla.databinding.ActivityChatBinding
import `in`.dragonbra.vapulla.extension.bold
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.isVisible
import `in`.dragonbra.vapulla.extension.normal
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.extension.toggleVisibility
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import `in`.dragonbra.vapulla.view.ChatView
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity :
    VapullaBaseActivity<ChatView, ChatPresenter>(),
    ChatView,
    TextWatcher,
    EmoteAdapter.EmoteListener {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    @Inject
    lateinit var chatMessageDao: ChatMessageDao

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    @Inject
    lateinit var emoticonDao: EmoticonDao

    @Inject
    lateinit var schemaManager: GameSchemaManager

    @Inject
    lateinit var clipboard: ClipboardManager

    private lateinit var paperPlane: PaperPlane

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var emoteAdapter: EmoteAdapter

    private lateinit var binding: ActivityChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.chatToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        paperPlane = PaperPlane(this, 18.0f)
        chatAdapter = ChatAdapter(this, paperPlane, clipboard)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
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

        val emoteLayoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
        }

        binding.emoteList.layoutManager = emoteLayoutManager
        binding.emoteList.adapter = emoteAdapter

        binding.messageBox.addTextChangedListener(this)
        binding.messageBox.requestFocus()
        binding.messageBox.setOnClickListener { binding.emoteList.hide() }

        binding.sendButton.click {
            val message = binding.messageBox.text.toString()

            if (message.isNotEmpty()) {
                binding.messageBox.setText("")
                presenter.sendMessage(message)
            }
        }

        binding.emoteButton.click {
            binding.emoteList.toggleVisibility()

            if (binding.emoteList.isVisible()) {
                Utils.hideKeyboardFrom(this, binding.messageBoxLayout)
                presenter.requestEmotes()
            }
        }

        binding.imageButton.click {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> navigateUp()
            R.id.menuViewProfile -> presenter.viewProfile()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun createPresenter(): ChatPresenter {
        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        return ChatPresenter(
            applicationContext,
            chatMessageDao,
            steamFriendDao,
            emoticonDao,
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

    override fun showChat(list: PagingData<ChatMessage>) {
        chatAdapter.submitData(lifecycle, list)
    }

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }
        runOnUiThread {
            val state = EPersonaState.from(friend.state ?: 0)

            if (!friend.nickname.isNullOrEmpty()) {
                // Has nickname
                binding.friendUsername.setTypeface(null, Typeface.ITALIC)
                binding.friendUsername.text = getString(R.string.nicknameFormat, friend.nickname)
            } else {
                // No nickname
                binding.friendUsername.text = friend.name
            }

            if ((
                friend.lastMessageTime == null ||
                    friend.typingTs > friend.lastMessageTime!!
                ) &&
                friend.typingTs > System.currentTimeMillis() - 15000L
            ) {
                binding.friendStatus.text = getString(R.string.statusTyping)
                binding.friendStatus.setTextColor(
                    ContextCompat.getColor(this@ChatActivity, R.color.colorAccent)
                )
                binding.friendStatus.bold()
            } else {
                binding.friendStatus.text =
                    Utils.getStatusText(
                        this@ChatActivity,
                        state,
                        friend.gameAppId,
                        friend.gameName,
                        friend.lastLogOff
                    )

                binding.friendStatus.setTextColor(
                    ContextCompat.getColor(this@ChatActivity, R.color.colorTyping)
                )
                binding.friendStatus.normal()
            }

            Glide.with(this@ChatActivity)
                .load(Utils.getAvatarUrl(friend.avatar))
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

    override fun viewProfile(steamID: Long) {
        startActivity(
            Intent(this, ProfileActivity::class.java).also {
                it.putExtra(ProfileActivity.INTENT_STEAM_ID, steamID)
            }
        )
    }

    override fun showEmotes(list: List<Emoticon>) {
        emoteAdapter.swap(list)
    }

    override fun onEmoteSelected(emoticon: Emoticon) {
        if (emoticon.isSticker) {
            presenter.sendMessage("/sticker ${emoticon.name}")
        } else {
            binding.messageBox.text.insert(binding.messageBox.selectionStart, ":${emoticon.name}:")
        }
    }
}
