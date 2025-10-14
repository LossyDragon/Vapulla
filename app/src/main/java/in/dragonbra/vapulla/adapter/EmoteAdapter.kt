package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.extension.click
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import `in`.dragonbra.vapulla.databinding.ListEmoteBinding

class EmoteAdapter(val context: Context, val listener: EmoteListener? = null) : RecyclerView.Adapter<EmoteAdapter.ViewHolder>() {

    var emoteList: List<Emoticon> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ListEmoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = emoteList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(emoteList[position])
    }

    fun swap(list: List<Emoticon>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition] == emoteList[oldItemPosition]

            override fun getOldListSize() = emoteList.size

            override fun getNewListSize() = list.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition] == emoteList[oldItemPosition]
        })
        emoteList = list
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(val v: ListEmoteBinding) : RecyclerView.ViewHolder(v.root) {
        fun bind(emote: Emoticon) {
            v.emote.click { listener?.onEmoteSelected(emote) }

            Glide.with(context)
                    .load("https://steamcommunity-a.akamaihd.net/economy/emoticon/:${emote.name}:")
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(v.emote)
        }
    }

    interface EmoteListener {
        fun onEmoteSelected(emoticon: Emoticon)
    }
}