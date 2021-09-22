package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.databinding.ListChatReceivedBinding
import `in`.dragonbra.vapulla.databinding.ListChatSentBinding
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.longClick
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.view.ChatLayout
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    val context: Context,
    val paperPlane: PaperPlane,
    val clipboard: ClipboardManager
) : PagingDataAdapter<ChatMessage, ChatAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        @SuppressLint("ConstantLocale")
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())

        const val VIEW_TYPE_RECEIVED = 0
        const val VIEW_TYPE_SENT = 1

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.message == newItem.message &&
                    oldItem.timestamp == newItem.timestamp &&
                    oldItem.fromLocal == newItem.fromLocal &&
                    oldItem.friendId == newItem.friendId
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_RECEIVED -> {
                val binding = ListChatReceivedBinding.inflate(inflater, parent, false)
                ViewHolder(binding)
            }
            VIEW_TYPE_SENT -> {
                val binding = ListChatSentBinding.inflate(inflater, parent, false)
                ViewHolder(binding)
            }
            else ->
                throw Exception("Chat Adapter view type was not of Received or Sent: $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        val showDate =
            position == (itemCount - 1) ||
                message?.formattedTs != getItem(position + 1)?.formattedTs

        if (message != null) {
            holder.bind(message, showDate)
        } else {
            holder.clear()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message == null || !message.fromLocal) {
            VIEW_TYPE_RECEIVED
        } else {
            VIEW_TYPE_SENT
        }
    }

    inner class ViewHolder : RecyclerView.ViewHolder {
        private var chatLayout: ChatLayout
        private var dateView: TextView
        private var messageView: TextView
        private var timeView: TextView

        constructor(binding: ListChatSentBinding) : super(binding.root) {
            chatLayout = binding.chatLayout
            dateView = binding.date
            messageView = binding.message
            timeView = binding.time
        }

        constructor(binding: ListChatReceivedBinding) : super(binding.root) {
            chatLayout = binding.chatLayout
            dateView = binding.date
            messageView = binding.message
            timeView = binding.time
        }

        fun bind(message: ChatMessage, showDate: Boolean) {
            paperPlane.load(messageView, message.message, showUrl = true, showStickers = true)

            // TO DO the spannable breaks events, the selector of the parent is still broken
            messageView.longClick {
                (it.parent as View).performLongClick()
            }

            chatLayout.longClick {
                val clip = ClipData.newPlainText("steam message", messageView.text)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, R.string.toastClipboard, Toast.LENGTH_SHORT).show()
                true
            }

            timeView.text = TIME_FORMAT.format(Date(message.timestamp))

            if (showDate) {
                dateView.show()
                dateView.text = message.formattedTs
            } else {
                dateView.hide()
            }
        }

        fun clear() {
            paperPlane.clear(messageView)
        }
    }
}
