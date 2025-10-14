package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.longClick
import `in`.dragonbra.vapulla.extension.show
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import `in`.dragonbra.vapulla.databinding.ListChatReceivedBinding
import `in`.dragonbra.vapulla.databinding.ListChatSentBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    val context: Context,
    val paperPlane: PaperPlane,
    val clipboard: ClipboardManager
) : PagedListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
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
        val v = when (viewType) {
            VIEW_TYPE_RECEIVED -> ListChatReceivedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            VIEW_TYPE_SENT -> ListChatSentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> ListChatReceivedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        val showDate = position == (itemCount - 1)
                || message?.formattedTs != getItem(position + 1)?.formattedTs
        if (message != null) {
            holder.bind(message, showDate)
        } else {
            holder.clear()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        if (message == null || !message.fromLocal) {
            return VIEW_TYPE_RECEIVED
        } else {
            return VIEW_TYPE_SENT
        }
    }

    inner class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage, showDate: Boolean) {
            when (binding) {
                is ListChatReceivedBinding -> {
                    paperPlane.load(binding.message, message.message, true)

                    // TODO the spannable breaks events, the selector of the parent is still broken
                    binding.message.longClick {
                        (it.parent as View).performLongClick()
                    }

                    binding.chatLayout.longClick {
                        val clip = ClipData.newPlainText("steam message", binding.message.text)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(context, R.string.toastClipboard, Toast.LENGTH_SHORT).show()
                        true
                    }

                    binding.time.text = TIME_FORMAT.format(Date(message.timestamp))

                    if (showDate) {
                        binding.date.show()
                        binding.date.text = message.formattedTs
                    } else {
                        binding.date.hide()
                    }
                }

                is ListChatSentBinding -> {
                    paperPlane.load(binding.message, message.message, true)

                    binding.message.longClick {
                        (it.parent as View).performLongClick()
                    }

                    binding.chatLayout.longClick {
                        val clip = ClipData.newPlainText("steam message", binding.message.text)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(context, R.string.toastClipboard, Toast.LENGTH_SHORT).show()
                        true
                    }

                    binding.time.text = TIME_FORMAT.format(Date(message.timestamp))

                    if (showDate) {
                        binding.date.show()
                        binding.date.text = message.formattedTs
                    } else {
                        binding.date.hide()
                    }
                }
            }
        }

        fun clear() {
            when (binding) {
                is ListChatReceivedBinding -> paperPlane.clear(binding.message)
                is ListChatSentBinding -> paperPlane.clear(binding.message)
            }
        }
    }
}