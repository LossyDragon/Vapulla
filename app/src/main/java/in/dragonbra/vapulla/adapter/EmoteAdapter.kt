package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.util.Utils.EMOTE_URL
import `in`.dragonbra.vapulla.util.Utils.STICKER_URL
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.penfeizhou.animation.apng.APNGDrawable
import kotlinx.android.synthetic.main.list_emote.view.*
import java.io.File


class EmoteAdapter(val context: Context, val listener: EmoteListener? = null) : RecyclerView.Adapter<EmoteAdapter.ViewHolder>() {

    var emoteList: List<Emoticon> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_emote, parent, false)
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

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(emote: Emoticon) {
            v.emote.click { listener?.onEmoteSelected(emote) }

            when (emote.isSticker) {
                true -> loadSticker(emote, v)
                false -> loadEmote(emote, v)
            }
        }
    }

    private fun loadSticker(emoticon: Emoticon, v: View) {
        // APNGDrawable's glide extension didn't seem to work. It loaded but no animation.
        // This was found on through issue #40
        Glide.with(context)
                .asFile()
                .load("$STICKER_URL${emoticon.name}")
                .addListener(object : RequestListener<File> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean = false

                    override fun onResourceReady(resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        //Only the original thread that created a view hierarchy can touch its views.
                        Handler(v.context.mainLooper).post {
                            v.emote.setImageDrawable(APNGDrawable.fromFile(resource!!.absolutePath))
                        }
                        return true
                    }
                })
                .submit()
    }

    private fun loadEmote(emoticon: Emoticon, v: View) {
        Glide.with(context)
                .load("$EMOTE_URL${emoticon.name}")
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(v.emote)
    }

    interface EmoteListener {
        fun onEmoteSelected(emoticon: Emoticon)
    }
}