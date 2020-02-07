package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_Notification
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceMethodCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceServiceMethodCallback

class VapullaHandler : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> handleEmoticonList(packetMsg)
            EMsg.ServiceMethod -> handleServiceMethod(packetMsg)
            EMsg.ServiceMethodResponse -> handleServiceServiceMethodResponse(packetMsg)
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

    fun sendMessage(steamID: SteamID, message: String) {
        val request = ClientMsgProtobuf<CFriendMessages_SendMessage_Request.Builder>(CFriendMessages_SendMessage_Request::class.java, EMsg.ServiceMethodCallFromClient).apply {
            protoHeader.targetJobName = "FriendMessages.SendMessage#1" //We MUST send the job name
            body.steamid = steamID.convertToUInt64()
            body.chatEntryType = EChatEntryType.ChatMsg.code()
            body.message = message
            body.containsBbcode = true
            body.echoToSender = false
            body.lowPriority = false
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

    //Not used yet
    private fun handleServiceServiceMethodResponse(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CFriendMessages_SendMessage_Response.Builder>(CFriendMessages_SendMessage_Response::class.java, packetMsg)
        client.postCallback(ServiceServiceMethodCallback(msg.body, msg.header.proto.targetJobName))
    }
}