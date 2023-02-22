package `in`.dragonbra.vapulla.steam

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChat_RequestFriendPersonaStates_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Request
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.steam.callback.RecentMessagesResponseCallback
import `in`.dragonbra.vapulla.steam.callback.SendMessageResponseCallback
import java.util.*

const val targetJobRecentMessages = "FriendMessages.GetRecentMessages#1"
const val targetJobSendMessage = "FriendMessages.SendMessage#1"
const val targetJobFriendPersonaStates = "Chat.RequestFriendPersonaStates#1"

class UnifiedChatHandler : ClientMsgHandler() {

    private var dispatchMap: EnumMap<EMsg, Consumer<IPacketMsg>> =
        EnumMap<EMsg, Consumer<IPacketMsg>>(EMsg::class.java)

    init {
        dispatchMap[EMsg.ServiceMethodResponse] =
            Consumer { packetMsg -> handleServiceServiceMethodResponse(packetMsg) }
    }

    override fun handleMsg(packetMsg: IPacketMsg) {
        val dispatcher = dispatchMap[packetMsg.msgType]
        dispatcher?.accept(packetMsg)
    }

    private fun handleServiceServiceMethodResponse(packetMsg: IPacketMsg?) {
        val horribleString = packetMsg?.data!!.decodeToString()

        when {
            horribleString.contains(targetJobRecentMessages) -> {
                val msg =
                    ClientMsgProtobuf<CFriendMessages_GetRecentMessages_Response.Builder>(
                        CFriendMessages_GetRecentMessages_Response::class.java,
                        packetMsg
                    )
                val resp = RecentMessagesResponseCallback(msg.body, msg.header.proto.targetJobName)
                client.postCallback(resp)
            }
            horribleString.contains(targetJobFriendPersonaStates) -> {
                // Refreshed FriendsList states.
            }
            horribleString.contains(targetJobSendMessage) -> {
                val msg =
                    ClientMsgProtobuf<CFriendMessages_SendMessage_Response.Builder>(
                        CFriendMessages_SendMessage_Response::class.java,
                        packetMsg
                    )
                val resp = SendMessageResponseCallback(msg.body, msg.header.proto.targetJobName)
                client.postCallback(resp)
            }
            else -> return
        }
    }

    fun sendMessage(steamID: SteamID, message: String) {
        val request =
            ClientMsgProtobuf<CFriendMessages_SendMessage_Request.Builder>(
                CFriendMessages_SendMessage_Request::class.java,
                EMsg.ServiceMethodCallFromClient
            ).apply {
                // We MUST send the job name
                protoHeader.targetJobName = targetJobSendMessage
                body.steamid = steamID.convertToUInt64()
                body.chatEntryType = EChatEntryType.ChatMsg.code()
                body.message = message
                body.containsBbcode = true
                body.echoToSender = false
                body.lowPriority = false
            }

        client.send(request)
    }

    // steamID1 = yourself, steamID2 = friend
    fun getRecentMessages(steamID1: Long, steamID2: SteamID) {
        val request =
            ClientMsgProtobuf<CFriendMessages_GetRecentMessages_Request.Builder>(
                CFriendMessages_GetRecentMessages_Request::class.java,
                EMsg.ServiceMethodCallFromClient
            ).apply {
                // We MUST send the job name
                protoHeader.targetJobName = targetJobRecentMessages
                body.steamid1 = steamID1
                body.steamid2 = steamID2.convertToUInt64()
                body.count = 50
                body.rtime32StartTime = 0
                body.mostRecentConversation = false
                body.bbcodeFormat = true
                body.startOrdinal = 0
                body.timeLast = 2147483647
                body.ordinalLast = 0
            }

        client.send(request)
    }

    // Manually get an updated list of your friends.
    fun getFriendsList() {
        val request =
            ClientMsgProtobuf<CChat_RequestFriendPersonaStates_Request.Builder>(
                CChat_RequestFriendPersonaStates_Request::class.java,
                EMsg.ServiceMethodCallFromClient
            )
        request.protoHeader.targetJobName = targetJobFriendPersonaStates

        client.send(request)
    }
}
