package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.databinding.ListFriendBinding
import `in`.dragonbra.vapulla.databinding.ListFriendRequestBinding
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.TextHeader
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler
import com.bumptech.glide.Glide
import com.mikhaellopez.circularimageview.CircularImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.*

class FriendListAdapter(
    val context: Context,
    val schemaManager: GameSchemaManager,
    val paperPlane: PaperPlane,
    val offlineStatusUpdater: OfflineStatusUpdater
) : RecyclerView.Adapter<FriendListAdapter.ViewHolder>(), StickyHeaderHandler {

    companion object {
        const val VIEW_TYPE_FRIEND_REQUEST = 1
        const val VIEW_TYPE_FRIEND = 2

        const val ITEM_TYPE_HEADER = 0
        const val ITEM_TYPE_FRIEND_REQUEST = 1
        const val ITEM_TYPE_FRIEND_OFFLINE = 2
        const val ITEM_TYPE_FRIEND_ONLINE = 3
        const val ITEM_TYPE_FRIEND_IN_GAME = 4
        const val ITEM_TYPE_FRIEND_RECENT = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var friendList: MutableList<Any> = LinkedList()

    var listener: OnItemSelectedListener? = null

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var updateTime = 0L

    private var recentsTimeout: Long = 0L

    private lateinit var sortPrefs: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> {
                val binding = ListFriendRequestBinding.inflate(inflater, parent, false)
                ViewHolder(binding)
            }
            VIEW_TYPE_FRIEND -> {
                val binding = ListFriendBinding.inflate(inflater, parent, false)
                ViewHolder(binding)
            }
            else ->
                throw Exception("Friend Adapter view type was not of Request or Friend: $viewType")
        }
    }

    override fun getItemCount(): Int = friendList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        friendList[position].let {
            holder.bind(it)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = friendList[position]
        if (item is FriendListItem) {
            if (item.isRequestRecipient()) {
                return VIEW_TYPE_FRIEND_REQUEST
            }
        }
        return VIEW_TYPE_FRIEND
    }

    override fun getAdapterData(): MutableList<*> = friendList

    fun swap(list: MutableList<FriendListItem>, updateTime: Long) {
        this.updateTime = updateTime

        recentsTimeout =
            prefs.getString("pref_friends_list_recents", "604800000")!!.toLong()

        sortPrefs =
            prefs.getString("pref_friends_list_sort", "1")!!

        var currentViewType = -1
        var newList: MutableList<Any> = mutableListOf()

        if (sortPrefs == "1") {
            newList = LinkedList(list)
            for (i in (list.size - 1) downTo 0) {
                val type = getItemType(newList[i])
                if (currentViewType == -1) {
                    currentViewType = type
                } else if (type != currentViewType) {
                    newList.add(i + 1, TextHeader(getHeader(currentViewType)))
                    currentViewType = type
                }
            }
            newList.add(0, TextHeader(getHeader(currentViewType)))
        } else {
            // Yeah, this could be done way better...
            var char = " "
            var request = false
            var recent = false

            list.forEach { item ->
                if (item.isRequestRecipient()) {
                    if (!request) newList.add(
                        TextHeader(context.getString(R.string.headerFriendRequest))
                    )
                    newList.add(item)
                    request = true
                    return@forEach
                }

                if (item.isItemRecentChat(recentsTimeout, updateTime)) {
                    if (!recent) newList.add(
                        TextHeader(context.getString(R.string.headerFriendRecent))
                    )
                    newList.add(item)
                    recent = true
                    return@forEach
                }

                val firstLetter = item.getFirstLetter()
                if (firstLetter != char) {
                    char = firstLetter
                    newList.add(TextHeader(char))
                    newList.add(item)
                } else {
                    newList.add(item)
                }
            }
        }

        val result = DiffUtil.calculateDiff(FriendDiffUtil(newList))
        friendList = newList
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder : RecyclerView.ViewHolder {
        private var avatar: CircularImageView
        private var friendLayout: ConstraintLayout? = null
        private var header: TextView? = null
        private var lastMessage: TextView? = null
        private var mobileIndicator: ImageView? = null
        private var moreButton: ImageView? = null
        private var newMessageCount: TextView? = null
        private var nickname: TextView? = null
        private var status: TextView? = null
        private var time: TextView? = null
        private var username: TextView? = null
        private var webIndicator: ImageView? = null

        constructor(binding: ListFriendRequestBinding) : super(binding.root) {
            avatar = binding.avatar
            moreButton = binding.moreButton
        }

        constructor(binding: ListFriendBinding) : super(binding.root) {
            avatar = binding.avatar
            friendLayout = binding.friendLayout
            header = binding.header
            mobileIndicator = binding.mobileIndicator
            newMessageCount = binding.newMessageCount
            nickname = binding.nickname
            status = binding.status
            time = binding.time
            lastMessage= binding.lastMessage
            username = binding.username
            webIndicator = binding.webIndicator
        }

        fun bind(item: Any) {

            (item as? TextHeader)?.let {
                header?.text = it.title
                showHeader()
            }

            (item as? FriendListItem)?.let { friend ->
                Glide.with(context)
                    .clear(avatar)

                Glide.with(context)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .apply(Utils.avatarOptions)
                    .into(avatar)

                when (friend.relation) {
                    EFriendRelationship.RequestRecipient.code() -> {
                        moreButton?.click {

                            val popup = PopupMenu(context, it)
                            popup.menuInflater.inflate(R.menu.menu_friend_request, popup.menu)
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.accept -> {
                                        listener?.onRequestAccept(friend)
                                        true
                                    }
                                    R.id.ignore -> {
                                        listener?.onRequestIgnore(friend)
                                        true
                                    }
                                    R.id.block -> {
                                        listener?.onRequestBlock(friend)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                    }
                    else -> {
                        showFriend()
                        if (friend.gameAppId > 0) {
                            scope.executeAsyncTask(
                                doInBackground = {
                                    schemaManager.touch(friend.gameAppId)
                                }
                            )
                        }

                        if (Strings.isNullOrEmpty(friend.nickname)) {
                            nickname?.hide()
                            nickname?.text = null
                        } else {
                            val nick = context.getString(R.string.nicknameFormat, friend.nickname)
                            nickname?.show()
                            nickname?.text = nick
                        }

                        val state = friend.state?.let { EPersonaState.from(it) }

                        if ((
                            friend.lastMessageTime == null ||
                                friend.typingTs > friend.lastMessageTime!!
                            ) &&
                            friend.typingTs > System.currentTimeMillis() - 20000L
                        ) {
                            offlineStatusUpdater.clear(status)
                            status?.text = context.getString(R.string.statusTyping)
                            status?.setTextColor(getColor(context, R.color.colorAccent))
                            status?.bold()
                        } else {
                            offlineStatusUpdater.schedule(status, friend)
                            status?.text =
                                Utils.getStatusText(
                                    context,
                                    state,
                                    friend.gameAppId,
                                    friend.gameName,
                                    friend.lastLogOff
                                )
                            status?.setTextColor(getColor(context, R.color.textSecondary))
                            status?.normal()
                        }

                        paperPlane.load(
                            view = lastMessage!!,
                            message = friend.lastMessage ?: "",
                            showUrl = false, showStickers = false
                        )

                        val newMessages: Int = friend.newMessageCount ?: 0
                        if (newMessages > 0) {
                            lastMessage?.setTextColor(getColor(context, R.color.textPrimary))
                            lastMessage?.bold()
                            newMessageCount?.text = newMessages.toString()
                            newMessageCount?.show()
                            username?.bold()
                        } else {
                            lastMessage?.setTextColor(getColor(context, R.color.textSecondary))
                            lastMessage?.normal()
                            newMessageCount?.hide()
                            username?.normal()
                        }

                        avatar.borderColor =
                            Utils.getStatusColor(
                                context,
                                state,
                                friend.gameAppId,
                                friend.gameName
                            )

                        mobileIndicator?.hide()
                        webIndicator?.hide()
                        val flags = EPersonaStateFlag.from(friend.stateFlags)
                        if (flags.contains(EPersonaStateFlag.ClientTypeMobile)) {
                            mobileIndicator?.show()
                        } else if (flags.contains(EPersonaStateFlag.ClientTypeWeb)) {
                            webIndicator?.show()
                        }

                        friend.lastMessageTime?.let {
                            time?.text =
                                DateUtils.formatSameDayTime(
                                    it,
                                    System.currentTimeMillis(),
                                    DateFormat.SHORT,
                                    DateFormat.SHORT
                                )
                            time?.show()
                        } ?: run {
                            time?.hide()
                        }

                        friendLayout?.click {
                            listener?.onItemSelected(friend)
                        }

                        friendLayout?.longClick {
                            listener?.onLongItemSelected(friend)
                            true
                        }
                    }
                }

                username?.text = friend.name
            }
        }

        private fun showHeader() {
            header?.show()
            friendLayout?.hide()
        }

        private fun showFriend() {
            header?.hide()
            friendLayout?.show()
        }
    }

    private fun getHeader(viewType: Int): String {
        return context.getString(
            when (viewType) {
                ITEM_TYPE_FRIEND_REQUEST -> R.string.headerFriendRequest
                ITEM_TYPE_FRIEND_OFFLINE -> R.string.headerFriendOffline
                ITEM_TYPE_FRIEND_ONLINE -> R.string.headerFriendOnline
                ITEM_TYPE_FRIEND_IN_GAME -> R.string.headerFriendInGame
                ITEM_TYPE_FRIEND_RECENT -> R.string.headerFriendRecent
                else -> R.string.headerFriendOffline
            }
        )
    }

    private fun getItemType(item: Any): Int {
        if (item is FriendListItem) {
            return when {
                item.isRequestRecipient() -> ITEM_TYPE_FRIEND_REQUEST
                item.isItemRecentChat(recentsTimeout, updateTime) -> ITEM_TYPE_FRIEND_RECENT
                item.isInGame() -> ITEM_TYPE_FRIEND_IN_GAME
                item.isOnline() -> ITEM_TYPE_FRIEND_ONLINE
                else -> ITEM_TYPE_FRIEND_OFFLINE
            }
        }
        return ITEM_TYPE_HEADER
    }

    inner class FriendDiffUtil(val list: MutableList<Any>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

            val newItem = list[newItemPosition]
            val oldItem = friendList[oldItemPosition]

            return newItem is FriendListItem &&
                oldItem is FriendListItem &&
                newItem.id == oldItem.id &&
                newItem.name == oldItem.name ||
                newItem is TextHeader &&
                oldItem is TextHeader &&
                newItem.title == oldItem.title
        }

        override fun getOldListSize() = friendList.size

        override fun getNewListSize() = list.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            list[newItemPosition] == friendList[oldItemPosition]
    }

    interface OnItemSelectedListener {
        fun onItemSelected(friend: FriendListItem)
        fun onLongItemSelected(friend: FriendListItem)
        fun onRequestAccept(friend: FriendListItem)
        fun onRequestIgnore(friend: FriendListItem)
        fun onRequestBlock(friend: FriendListItem)
    }
}
