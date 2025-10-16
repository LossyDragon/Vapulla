package `in`.dragonbra.vapulla.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.TextHeader
import java.text.DateFormat
import java.util.*


class FriendListAdapter(
    val context: Context, val schemaManager: GameSchemaManager,
    val paperPlane: PaperPlane, val offlineStatusUpdater: OfflineStatusUpdater
) :
    RecyclerView.Adapter<FriendListAdapter.ViewHolder>(), StickyHeaderHandler {

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

    var friendList: MutableList<Any> = LinkedList()

    var listener: OnItemSelectedListener? = null

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var updateTime = 0L

    private var recentsTimeout =
        prefs.getString("pref_friends_list_recents", "604800000")!!.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> ListFriendRequestBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> ListFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
        return ViewHolder(v)
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
            if (item.relation == EFriendRelationship.RequestRecipient.code()) {
                return VIEW_TYPE_FRIEND_REQUEST
            }
        }
        return VIEW_TYPE_FRIEND
    }

    private fun getItemType(item: Any): Int {
        if (item is FriendListItem) {
            return if (item.relation == EFriendRelationship.RequestRecipient.code()) {
                ITEM_TYPE_FRIEND_REQUEST
            } else if (recentsTimeout == 0L || (recentsTimeout > 0L && item.lastMessageTime?.let { it >= updateTime - recentsTimeout } == true)) {
                ITEM_TYPE_FRIEND_RECENT
            } else if (item.isPlayingGame) {
                ITEM_TYPE_FRIEND_IN_GAME
            } else if (item.isOnline) {
                ITEM_TYPE_FRIEND_ONLINE
            } else ITEM_TYPE_FRIEND_OFFLINE
        }
        return ITEM_TYPE_HEADER
    }

    override fun getAdapterData(): MutableList<*> = friendList

    fun swap(list: List<FriendListItem>, updateTime: Long) {
        this.updateTime = updateTime
        recentsTimeout = prefs.getString("pref_friends_list_recents", "604800000")!!.toLong()

        var currentViewType = -1
        val newList: MutableList<Any> = LinkedList(list)

        if (!newList.isEmpty()) {
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
        }

        val result = DiffUtil.calculateDiff(FriendDiffUtil(newList))
        friendList = newList
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Any) {

            (item as? TextHeader)?.let {
                when (binding) {
                    is ListFriendBinding -> {
                        binding.header.text = it.title
                        showHeader()
                    }

                    is ListFriendRequestBinding -> {
                        binding.username.text = it.title
                        showHeader()
                    }
                }
            }

            (item as? FriendListItem)?.let { friend ->
                when (binding) {
                    is ListFriendRequestBinding -> bindFriendRequest(friend)
                    is ListFriendBinding -> bindFriend(friend)
                }

                // Common binding for both types
                getUsername().text = friend.name
            }
        }

        private fun bindFriendRequest(friend: FriendListItem) {
            val b = binding as ListFriendRequestBinding

            Glide.with(context)
                .clear(b.avatar)

            Glide.with(context)
                .load(Utils.getAvatarURL(friend.avatar))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(Utils.avatarOptions)
                .into(b.avatar)

            b.moreButton.click {
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

        private fun bindFriend(friend: FriendListItem) {
            val b = binding as ListFriendBinding

            Glide.with(context)
                .clear(b.avatar)

            Glide.with(context)
                .load(Utils.getAvatarURL(friend.avatar))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(Utils.avatarOptions)
                .into(b.avatar)

            showFriend()

            if (friend.gameAppId > 0) {
                runOnBackgroundThread {
                    schemaManager.touch(friend.gameAppId)
                }
            }

            if (Strings.isNullOrEmpty(friend.nickname)) {
                b.nickname.hide()
                b.nickname.text = null
            } else {
                b.nickname.show()
                b.nickname.text = context.getString(R.string.nicknameFormat, friend.nickname)
            }

            val state = friend.state?.let { EPersonaState.from(it) }

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                && friend.typingTs > System.currentTimeMillis() - 20000L
            ) {
                offlineStatusUpdater.clear(b.status)
                b.status.text = context.getString(R.string.statusTyping)
                val color = ContextCompat.getColor(context, R.color.colorAccent)
                b.status.setTextColor(color)
                b.status.bold()
            } else {
                offlineStatusUpdater.schedule(b.status, friend)
                b.status.text = Utils.getStatusText(
                    context,
                    state,
                    friend.gameAppId,
                    friend.gameName,
                    friend.lastLogOff
                )
                val color = ContextCompat.getColor(context, R.color.textSecondary)
                b.status.setTextColor(color)
                b.status.normal()
            }

            paperPlane.load(b.lastMessage, friend.lastMessage ?: "", false)

            val newMessages: Int = friend.newMessageCount ?: 0
            if (newMessages > 0) {
                val color = ContextCompat.getColor(context, R.color.textPrimary)
                b.lastMessage.setTextColor(color)
                b.lastMessage.bold()
                b.newMessageCount.text = newMessages.toString()
                b.newMessageCount.show()
                b.username.bold()
            } else {
                val color = ContextCompat.getColor(context, R.color.textSecondary)
                b.lastMessage.setTextColor(color)
                b.lastMessage.normal()
                b.newMessageCount.hide()
                b.username.normal()
            }

            // (b.statusIndicator.drawable as GradientDrawable).setColor(
            //     // Utils.getStatusColor(
            //     //     context,
            //     //     state,
            //     //     friend.gameAppId,
            //     //     friend.gameName
            //     // )
            // )

            b.mobileIndicator.hide()
            b.webIndicator.hide()
            val flags = EPersonaStateFlag.from(friend.stateFlags)
            if (flags.contains(EPersonaStateFlag.ClientTypeMobile)) {
                b.mobileIndicator.show()
            } else if (flags.contains(EPersonaStateFlag.ClientTypeWeb)) {
                b.webIndicator.show()
            }

            friend.lastMessageTime?.let {
                b.time.text = DateUtils.formatSameDayTime(
                    it,
                    System.currentTimeMillis(),
                    DateFormat.SHORT,
                    DateFormat.SHORT
                )
                b.time.show()
            } ?: run {
                b.time.hide()
            }

            b.friendLayout.click {
                listener?.onItemSelected(friend)
            }
        }

        private fun showHeader() {
            when (binding) {
                is ListFriendBinding -> {
                    binding.header.show()
                    binding.friendLayout.hide()
                }

                is ListFriendRequestBinding -> {
                    binding.moreButton.show()
                    // friendLayout might not exist in request layout
                }
            }
        }

        private fun showFriend() {
            when (binding) {
                is ListFriendBinding -> {
                    binding.header.hide()
                    binding.friendLayout.show()
                }
            }
        }

        private fun getUsername() = when (binding) {
            is ListFriendBinding -> binding.username
            is ListFriendRequestBinding -> binding.username
            else -> throw IllegalStateException("Unknown binding type")
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

    inner class FriendDiffUtil(val list: MutableList<Any>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

            val newItem = list[newItemPosition]
            val oldItem = friendList[oldItemPosition]

            return newItem is FriendListItem && oldItem is FriendListItem && newItem.id == oldItem.id ||
                    newItem is TextHeader && oldItem is TextHeader && newItem.title == oldItem.title
        }

        override fun getOldListSize() = friendList.size

        override fun getNewListSize() = list.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            list[newItemPosition] == friendList[oldItemPosition]
    }

    interface OnItemSelectedListener {
        fun onItemSelected(friend: FriendListItem)
        fun onRequestAccept(friend: FriendListItem)
        fun onRequestIgnore(friend: FriendListItem)
        fun onRequestBlock(friend: FriendListItem)
    }
}