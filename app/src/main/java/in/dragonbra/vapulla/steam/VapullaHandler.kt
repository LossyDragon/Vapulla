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

    private var dispatchMap = EnumMap<EMsg, Consumer<IPacketMsg>>(EMsg::class.java)

    init {
        dispatchMap[EMsg.ClientEmoticonList] = Consumer { handleEmoticonList(it) }
    }

    override fun handleMsg(packetMsg: IPacketMsg) {
        dispatchMap[packetMsg.msgType]?.accept(packetMsg)
    }

    fun getEmoticonList() {
        ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
            CMsgClientGetEmoticonList::class.java,
            EMsg.ClientGetEmoticonList
        ).also(client::send)
    }

    private fun handleEmoticonList(packetMsg: IPacketMsg) {
        ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
            CMsgClientEmoticonList::class.java,
            packetMsg
        ).also {
            val callback = EmoticonListCallback(it.body)
            client.postCallback(callback)
        }
    }
}
