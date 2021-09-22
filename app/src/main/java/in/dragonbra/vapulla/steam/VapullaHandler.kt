package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.*
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceMethodCallback
import java.util.*

class VapullaHandler : ClientMsgHandler() {

    private var dispatchMap: EnumMap<EMsg, Consumer<IPacketMsg>> =
        EnumMap<EMsg, Consumer<IPacketMsg>>(EMsg::class.java)

    init {
        dispatchMap[EMsg.ClientEmoticonList] =
            Consumer { packetMsg -> handleEmoticonList(packetMsg) }
        dispatchMap[EMsg.ServiceMethod] =
            Consumer { packetMsg -> handleServiceMethod(packetMsg) }
    }

    override fun handleMsg(packetMsg: IPacketMsg) {
        val dispatcher = dispatchMap[packetMsg.msgType]
        dispatcher?.accept(packetMsg)
    }

    // Set the UIMode so that we can receive Unified callbacks
    // Send this on a sucessful LoggedOn to enable 'new unified' steam features.
    fun setClientUIMode() {
        // TODO find callback response
        val request = ClientMsgProtobuf<SteammessagesClientserver2.CMsgClientUIMode.Builder>(
            SteammessagesClientserver2.CMsgClientUIMode::class.java, EMsg.ClientCurrentUIMode
        ).apply {
            body.chatMode = 2
        }
        client.send(request)
    }

    fun getEmoticonList() {
        val request = ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
            CMsgClientGetEmoticonList::class.java, EMsg.ClientGetEmoticonList
        )

        client.send(request)
    }

    private fun handleEmoticonList(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
            CMsgClientEmoticonList::class.java, packetMsg
        )

        client.postCallback(EmoticonListCallback(msg.body))
    }

    private fun handleServiceMethod(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CFriendMessages_IncomingMessage_Notification.Builder>(
            CFriendMessages_IncomingMessage_Notification::class.java, packetMsg
        )

        client.postCallback(ServiceMethodCallback(msg.body, msg.header.proto.targetJobName))
    }
}
