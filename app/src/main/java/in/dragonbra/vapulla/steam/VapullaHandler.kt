package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback

class VapullaHandler : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> handleEmoticonList(packetMsg)
            else -> {
            }
        }
    }

    // Set the UIMode so that we can receive Unified callbacks
    // Send this on onLoggedIn to enable 'new' steam features.
    fun setClientUIMode() {
        val request = ClientMsgProtobuf<SteammessagesClientserver2.CMsgClientUIMode.Builder>(
                SteammessagesClientserver2.CMsgClientUIMode::class.java, EMsg.ClientCurrentUIMode).apply {
            body.chatMode = 2
        }
        client.send(request)
    }


    fun getEmoticonList() {
        val request = ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
                CMsgClientGetEmoticonList::class.java, EMsg.ClientGetEmoticonList)

        client.send(request)
    }

    private fun handleEmoticonList(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
                CMsgClientEmoticonList::class.java, packetMsg)

        client.postCallback(EmoticonListCallback(msg.body))
    }
}