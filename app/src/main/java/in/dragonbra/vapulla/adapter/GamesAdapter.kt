package `in`.dragonbra.vapulla.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.databinding.ListGamesBinding
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.debug
import java.math.RoundingMode
import java.text.DecimalFormat

class GamesAdapter : RecyclerView.Adapter<GamesAdapter.ViewHolder>() {

    companion object {
        const val SORT_ALPHABETICAL = 0
        const val SORT_PLAYTIME = 1
    }

    private var gamesList: MutableList<Games> = mutableListOf()

    lateinit var onOverflow: ((game: Games) -> Unit)

    override fun getItemCount(): Int = gamesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListGamesBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        gamesList[position].let {
            holder.bind(it)
        }
    }

    fun setList(list: MutableList<Games>, sort: Int) {
        if (sort == SORT_ALPHABETICAL) {
            debug("setList() -> l1, l2 = A-Z sort")
            list.sortWith { l1, l2 ->
                l1.name.compareTo(l2.name)
            }
        } else if (sort == SORT_PLAYTIME) {
            debug("setList() -> l2, l1 = 9-0 sort.")
            list.sortWith { l2, l1 ->
                l1.playtime_forever.compareTo(l2.playtime_forever)
            }
        }

        val diffResult = DiffUtil.calculateDiff(GamesDiffCallback(gamesList, list))
        gamesList.clear()
        gamesList.addAll(list)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatTime(time: Int): Double {
        var value: Double
        DecimalFormat("#.#").run {
            roundingMode = RoundingMode.CEILING
            value = time.div(60f).toDouble()
        }

        return value
    }

    private fun formatGameBanner(imageUrl: String?, appId: Int): String? {
        return if (imageUrl.isNullOrEmpty()) {
            null
        } else {
            String.format(Utils.GAME_LOGO_URL, appId, imageUrl)
        }
    }

    inner class ViewHolder(val v: ListGamesBinding) : RecyclerView.ViewHolder(v.root) {
        fun bind(item: Games) {

            Glide.with(v.root)
                .clear(v.gamesImage)

            Glide.with(v.root)
                .load(formatGameBanner(item.img_icon_url, item.appid))
                .error(R.drawable.vapulla)
                .into(v.gamesImage)

            v.gamesTitle.text = item.name

            v.gamesHoursForever.text = v.root.context.getString(
                R.string.textPlayedForever, formatTime(item.playtime_forever)
            )

            if (item.playtime_2weeks != null) {
                val time = formatTime(item.playtime_2weeks)
                val text = v.root.context.getString(R.string.textPlayedRecent, time)
                v.gamesHoursRecent.text = text
                v.gamesHoursRecent.show()
            }

            v.gamesMoreButton.click {
                onOverflow.invoke(item)
            }
        }
    }

    inner class GamesDiffCallback(
        private val oldList: List<Games>,
        private val newList: List<Games>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
