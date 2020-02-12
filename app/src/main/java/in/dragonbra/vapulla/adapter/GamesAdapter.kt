package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.VapullaLogger
import `in`.dragonbra.vapulla.util.debug
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.list_games.view.*
import java.math.RoundingMode
import java.text.DecimalFormat

class GamesAdapter(val context: Context) :
        RecyclerView.Adapter<GamesAdapter.ViewHolder>(),
        VapullaLogger {

    companion object {
        const val SORT_ALPHABETICAL = 0
        const val SORT_PLAYTIME = 1
    }

    var listener: OnItemSelectedListener? = null

    private var gamesList: List<Games> = listOf()

    override fun getItemCount(): Int = gamesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_games, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        gamesList[position].let {
            holder.bind(it)
        }
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: Games) {

            Glide.with(context)
                    .clear(v.findViewById<ImageView>(R.id.games_image))

            Glide.with(context)
                    .load(formatGameBanner(item.img_logo_url!!, item.appid))
                    .into(v.findViewById(R.id.games_image))

            v.findViewById<TextView>(R.id.games_title).text = item.name

            v.games_hours_forever.text = context.getString(
                    R.string.textPlayedForever, formatTime(item.playtime_forever))

            if (item.playtime_2weeks != null) {
                v.findViewById<TextView>(R.id.games_hours_recent).apply {
                    text = context.getString(
                            R.string.textPlayedRecent, formatTime(item.playtime_2weeks))
                    show()
                }
            }

            v.findViewById<ImageView>(R.id.games_more_button).click {
                listener?.onMoreItemSelected(item)
            }
        }
    }

    fun setList(list: MutableList<Games>, sort: Int) {
        if (sort == SORT_ALPHABETICAL) {
            debug("setList() -> l1, l2 = A-Z sort")
            list.sortWith(Comparator { l1, l2 ->
                l1.name.compareTo(l2.name)
            })
        } else if (sort == SORT_PLAYTIME) {
            debug("setList() -> l2, l1 = 9-0 sort.")
            list.sortWith(Comparator { l2, l1 ->
                l1.playtime_forever.compareTo(l2.playtime_forever)
            })
        }

        gamesList = list

        notifyDataSetChanged()
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

    interface OnItemSelectedListener {
        fun onMoreItemSelected(game: Games)
    }
}
