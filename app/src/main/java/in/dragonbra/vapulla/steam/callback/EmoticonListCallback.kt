package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class EmoticonListCallback(private val msg: CMsgClientEmoticonList.Builder) : CallbackMsg() {
    private var emoticons: MutableList<Emoticon> = mutableListOf()
    fun getEmoteList(): List<Emoticon> {
        msg.emoticonsList.forEach { emoticons.add(Emoticon(it)) }
        msg.stickersList.forEach { emoticons.add(Emoticon(it)) }
        return emoticons.toList()
    }
}

data class Emoticon(val name: String, val count: Int, val isSticker: Boolean) {
    constructor(emoticon: CMsgClientEmoticonList.Emoticon) :
            this(emoticon.name, emoticon.count, false)

    constructor(emoticon: CMsgClientEmoticonList.Sticker) :
            this(emoticon.name, emoticon.count, true)
}
