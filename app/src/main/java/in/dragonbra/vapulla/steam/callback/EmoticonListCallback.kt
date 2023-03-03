package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class EmoticonListCallback(private val msg: CMsgClientEmoticonList.Builder) : CallbackMsg() {
    private var emoticons: List<Emoticon> = listOf()
    private var stickers: List<Emoticon> = listOf()
    fun getEmoteList(): List<Emoticon> {
        emoticons.map { msg.emoticonsList }
        stickers.map { msg.stickersList }
        return emoticons.plus(stickers)
    }
}

data class Emoticon(val name: String, val isSticker: Boolean, val appId: Int) {
    constructor(emoticon: CMsgClientEmoticonList.Emoticon) :
        this(emoticon.name, false, emoticon.appid)

    constructor(emoticon: CMsgClientEmoticonList.Sticker) :
        this(emoticon.name, true, emoticon.appid)
}
