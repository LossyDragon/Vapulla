package `in`.dragonbra.vapulla.service.handler

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback

class VapullaHandler : ClientMsgHandler() {

    fun getEmoticonList() {
        val request =
            ClientMsgProtobuf<SteammessagesClientserverFriends.CMsgClientGetEmoticonList.Builder>(
                SteammessagesClientserverFriends.CMsgClientGetEmoticonList::class.java,
                EMsg.ClientGetEmoticonList
            )
        client.send(request)
    }

    override fun handleMsg(packetMsg: IPacketMsg) {
        when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> handleEmoticonList(packetMsg)
            else -> {
            }
        }
    }

    private fun handleEmoticonList(packetMsg: IPacketMsg) {
        val msg =
            ClientMsgProtobuf<SteammessagesClientserverFriends.CMsgClientEmoticonList.Builder>(
                SteammessagesClientserverFriends.CMsgClientEmoticonList::class.java,
                packetMsg
            )
        client.postCallback(EmoticonListCallback(msg.body))
    }
}