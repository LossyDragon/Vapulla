package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.retrofit.ImageRequestBody
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.info
import `in`.dragonbra.vapulla.view.ChatView
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import java.io.ByteArrayOutputStream

class ChatPresenter(context: Context,
                    private val chatMessageDao: ChatMessageDao,
                    private val steamFriendsDao: SteamFriendDao,
                    private val emoticonDao: EmoticonDao,
                    private val imgurAuthService: ImgurAuthService,
                    private val schemaManager: GameSchemaManager,
                    private val steamId: SteamID
) : VapullaPresenter<ChatView>(context) {

    companion object {
        const val UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS
        const val TYPING_INTERVAL = DateUtils.SECOND_IN_MILLIS * 20
    }

    private var lastTypingMessage = 0L

    private lateinit var chatData: LiveData<PagedList<ChatMessage>>

    private lateinit var friendData: LiveData<FriendListItem>

    private lateinit var emoticonData: LiveData<List<Emoticon>>

    private var emoteSet: Set<String> = setOf()

    private val updateHandler: Handler = Handler()

    private val chatObserver = Observer<PagedList<ChatMessage>> { list ->
        ifViewAttached { it.showChat(list) }
    }

    private val friendObserver = Observer<FriendListItem> { friend ->
        ifViewAttached { v ->
            friend?.let {
                if (it.relation == EFriendRelationship.Friend.code()) {
                    v.updateFriendData(friend)
                } else {
                    v.navigateUp()
                }
            }
        }
    }

    private val emoteObserver = Observer<List<Emoticon>> { list ->
        ifViewAttached { it.showEmotes(list ?: emptyList()) }
        emoteSet = (list ?: emptyList()).map { it.name }.toSet()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")

        steamService?.setChatFriendId(steamId)
        steamService?.isActivityRunning = true
        getMessageHistory()
    }

    override fun onPostCreate() {
        chatData = LivePagedListBuilder(
                chatMessageDao.findLivePaged(steamId.convertToUInt64()), 50).build()
        // chatData.observe(view as ChatActivity, chatObserver)
        ifViewAttached { chatData.observe(it as ChatActivity, chatObserver) }

        friendData = steamFriendsDao.findLive(steamId.convertToUInt64())
        // friendData.observe(view as ChatActivity, friendObserver)
        ifViewAttached { friendData.observe(it as ChatActivity, friendObserver) }

        friendData.value?.let {
            if (it.gameAppId > 0) {
                runOnBackgroundThread {
                    schemaManager.touch(it.gameAppId)
                }
            }
        }

        emoticonData = emoticonDao.getLive()
        // emoticonData.observe(view as ChatActivity, emoteObserver)
        ifViewAttached { emoticonData.observe(it as ChatActivity, emoteObserver) }

        ifViewAttached {
            it.showChat(chatData.value)
            it.updateFriendData(friendData.value)
            it.showEmotes(emoticonData.value ?: emptyList())
        }

        emoteSet = (emoticonData.value ?: emptyList()).map { it.name }.toSet()
    }

    override fun onResume() {
        if (bound) {
            steamService?.setChatFriendId(steamId)
            steamService?.isActivityRunning = true
            getMessageHistory()
        }

        updateFriend()

        runOnBackgroundThread {
            chatMessageDao.markRead(steamId.convertToUInt64())
        }
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
            steamService?.removeChatFriendId()
        }

        updateHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        chatData.removeObserver(chatObserver)
        friendData.removeObserver(friendObserver)
        emoticonData.removeObserver(emoteObserver)
    }

    private fun updateFriend() {
        ifViewAttached { it.updateFriendData(friendData.value) }
        updateHandler.postDelayed({ updateFriend() }, UPDATE_INTERVAL)
    }

    private fun getMessageHistory() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.requestMessageHistory(steamId)
            steamService?.getMessageHistory(steamId)
        }
    }

    override fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
    }

    fun sendMessage(message: String) {
        if (Strings.isNullOrEmpty(message)) {
            return
        }
        lastTypingMessage = 0L

        runOnBackgroundThread {
            steamService?.sendMessage(steamId, message, emoteSet)
        }
    }

    fun typing() {
        if (lastTypingMessage < System.currentTimeMillis() - TYPING_INTERVAL) {
            lastTypingMessage = System.currentTimeMillis()

            runOnBackgroundThread {
                steamService
                        ?.getHandler<SteamFriends>()
                        ?.sendChatMessage(steamId, EChatEntryType.Typing, "")
            }
        }
    }

    fun viewProfile() {
        ifViewAttached {
            it.viewProfile(steamId.convertToUInt64())
        }
    }

    fun requestEmotes() {
        runOnBackgroundThread {
            steamService?.getHandler<VapullaHandler>()?.getEmoticonList()
        }
    }

    fun imageButtonClicked() {
        if (imgurAuthService.authorized()) {
            imgurAuthService.refreshTokenIfNeeded()
            ifViewAttached {
                it.showPhotoSelector()
            }
        } else {
            ifViewAttached {
                it.showImgurDialog()
            }
        }
    }

    fun sendImage(image: Uri) {
        ifViewAttached {
            it.showUploadDialog()
        }
        runOnBackgroundThread {
            val bitmap = if (Utils.isGreaterThanP) {
                val source = ImageDecoder.createSource(context.contentResolver, image)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, image)
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            bitmap.recycle()
            val body = ImageRequestBody(baos.toByteArray()) { total, progress ->
                ifViewAttached { it.imageUploadProgress(total, progress) }
            }

            val call = imgurAuthService.postImage(body)

            val response = call.execute()

            if (response.isSuccessful) {
                ifViewAttached {
                    val responseBody = response.body()

                    if (responseBody != null) {
                        sendMessage(responseBody.data.link)
                        it.imageUploadSuccess()
                    } else {
                        it.imageUploadFail()
                    }
                }
            } else {
                ifViewAttached { it.imageUploadFail() }
            }
        }
    }
}
