package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import androidx.paging.PagingData
import com.hannesdorfmann.mosby3.mvp.MvpView

interface ChatView : MvpView {
    fun closeApp()
    fun showChat(list: PagingData<ChatMessage>)
    fun updateFriendData(friend: FriendListItem?)
    fun navigateUp()
    fun showEmotes(list: List<Emoticon>)
    fun viewProfile(steamID: Long)
}
