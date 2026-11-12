package `in`.dragonbra.vapulla.service.handler

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback

/**
 * App-specific JavaSteam handler for messages the library does not cover itself.
 * Currently that is only the emoticon list.
 */
class VapullaHandler : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        val callback = getCallback(packetMsg) ?: return
        client.postCallback(callback)
    }

    /** Asks Steam for the account's emoticons; answered with an [EmoticonListCallback]. */
    fun getEmoticonList() {
        ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
            CMsgClientGetEmoticonList::class.java,
            EMsg.ClientGetEmoticonList,
        ).also(client::send)
    }

    private companion object {
        fun getCallback(packetMsg: IPacketMsg): CallbackMsg? = when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> EmoticonListCallback(packetMsg)
            else -> null
        }
    }
}
