package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_NotificationOrBuilder
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.SteamID

class ServiceMethodCallback(
    msg: CFriendMessages_IncomingMessage_NotificationOrBuilder,
    targetJobName: String
) : CallbackMsg() {
    val entryType: EChatEntryType = EChatEntryType.from(msg.chatEntryType)
    val jobName: String = targetJobName // Header -> Proto -> getTargetJobName()
    val message: String = msg.message
    val steamID: SteamID = SteamID(msg.steamidFriend)
}

class SendMessageResponseCallback(
    msg: CFriendMessages_SendMessage_Response.Builder,
    targetJobName: String
) : CallbackMsg() {
    var jobName: String? = targetJobName // Header -> Proto -> getTargetJobName()
    var modifiedMessage: String? = msg.modifiedMessage
    var timestamp: Int? = msg.serverTimestamp
}

class RecentMessagesResponseCallback(
    msg: CFriendMessages_GetRecentMessages_Response.Builder,
    targetJobName: String
) : CallbackMsg() {
    var jobName: String? = targetJobName // Header -> Proto -> getTargetJobName()
    var messageHistory: Array<CFriendMessages_GetRecentMessages_Response.FriendMessage>? =
        msg.messagesList.toTypedArray()
}
