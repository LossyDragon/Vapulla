package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Response
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Response
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class ServiceServiceMethodCallback() : CallbackMsg() {

    var jobName: String? = null // Header -> Proto -> getTargetJobName()
    var modifiedMessage: String? = null
    var timestamp: Int? = null

    var messageHistory: Array<CFriendMessages_GetRecentMessages_Response.FriendMessage>? = null

    constructor(msg: CFriendMessages_SendMessage_Response.Builder,
                targetJobName: String
    ) : this() {
        jobName = targetJobName
        modifiedMessage = msg.modifiedMessage
        timestamp = msg.serverTimestamp
    }

    constructor(msg: CFriendMessages_GetRecentMessages_Response.Builder,
                targetJobName: String
    ) : this() {
        jobName = targetJobName
        messageHistory = msg.messagesList.toTypedArray()
    }
}
