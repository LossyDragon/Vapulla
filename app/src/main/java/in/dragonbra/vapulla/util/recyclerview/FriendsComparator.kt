package `in`.dragonbra.vapulla.util.recyclerview

import `in`.dragonbra.vapulla.adapter.FriendListItem
import android.content.Context
import androidx.preference.PreferenceManager

class FriendsComparator(context: Context, private val updateTime: Long) :
    Comparator<FriendListItem> {

    private val recentsTimeout: Long

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        recentsTimeout = prefs.getString("pref_friends_list_recents", "604800000")!!.toLong()
    }

    override fun compare(o1: FriendListItem, o2: FriendListItem): Int {
        /* Relation */
        if (o1.relation != o2.relation)
            return o1.relation.compareTo(o2.relation)

        /* Recent Chat */
        val recent1 = o1.isItemRecentChat(recentsTimeout, updateTime)
        val recent2 = o2.isItemRecentChat(recentsTimeout, updateTime)
        if (recent1 != recent2)
            return recent1.compareTo(recent2)

        /* Status */
        if (o1.isInGame() != o2.isInGame())
            return o1.isInGame().compareTo(o2.isInGame())

        if (o1.isInGameAwayOrSnooze() != o2.isInGameAwayOrSnooze())
            return o1.isInGameAwayOrSnooze().compareTo(o2.isInGameAwayOrSnooze())

        if (o1.isAwayOrSnooze() != o2.isAwayOrSnooze())
            return o1.isAwayOrSnooze().compareTo(o2.isAwayOrSnooze())

        /* Names */
        if (o1.friendName != o2.friendName)
            return o1.friendName.compareTo(o2.friendName)

        return 0
    }
}