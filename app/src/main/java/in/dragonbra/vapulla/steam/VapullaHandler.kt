package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import java.util.EnumMap

class VapullaHandler : ClientMsgHandler() {

    private var dispatchMap: EnumMap<EMsg, Consumer<IPacketMsg>> =
        EnumMap<EMsg, Consumer<IPacketMsg>>(EMsg::class.java)

    init {
        dispatchMap[EMsg.ClientEmoticonList] =
            Consumer { packetMsg -> handleEmoticonList(packetMsg) }
    }

    override fun handleMsg(packetMsg: IPacketMsg) {
        val dispatcher = dispatchMap[packetMsg.msgType]
        dispatcher?.accept(packetMsg)
    }

    fun getEmoticonList() {
        val request = ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
            CMsgClientGetEmoticonList::class.java,
            EMsg.ClientGetEmoticonList
        )

        client.send(request)
    }

    private fun handleEmoticonList(packetMsg: IPacketMsg) {
        val msg = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
            CMsgClientEmoticonList::class.java,
            packetMsg
        )

        client.postCallback(EmoticonListCallback(msg.body))
    }
}
