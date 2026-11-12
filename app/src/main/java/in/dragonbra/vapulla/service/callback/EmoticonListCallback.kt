package `in`.dragonbra.vapulla.service.callback

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.vapulla.db.entity.Emoticon

/**
 * The emoticons and stickers the account owns, from `ClientEmoticonList`. Emoticon names
 * arrive wrapped in colons (`:steamhappy:`); the wrapper is stripped here.
 */
class EmoticonListCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val emoteList: List<Emoticon>

    init {
        val resp = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
            CMsgClientEmoticonList::class.java,
            packetMsg,
        )
        jobID = resp.targetJobID

        val emoticons = resp.body.emoticonsList.map {
            Emoticon(name = it.name.removeSurrounding(":"), appID = it.appid, isSticker = false)
        }
        val stickers = resp.body.stickersList.map {
            Emoticon(name = it.name, appID = it.appid, isSticker = true)
        }
        emoteList = emoticons + stickers
    }
}
