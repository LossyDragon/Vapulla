package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_Notification
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceMethodCallback

class VapullaHandler : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> handleEmoticonList(packetMsg)
            EMsg.ServiceMethod -> handleServiceMethod(packetMsg)
            else -> {
            }
        }
    }

    // Set the UIMode so that we can receive Unified callbacks
    // Send this on onLoggedOn to enable 'new' steam features.
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

    private fun handleServiceMethod(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CFriendMessages_IncomingMessage_Notification.Builder>(
                CFriendMessages_IncomingMessage_Notification::class.java, packetMsg)

        client.postCallback(ServiceMethodCallback(msg.body, msg.header.proto.targetJobName))
    }
}