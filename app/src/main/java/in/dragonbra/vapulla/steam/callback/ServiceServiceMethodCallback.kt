package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class ServiceServiceMethodCallback(
        msg: SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Response.Builder,
        targetJobName: String
) : CallbackMsg() {
    val modifiedMessage: String = msg.modifiedMessage
    val timestamp: Int = msg.serverTimestamp
    val jobName: String = targetJobName // Header -> Proto -> getTargetJobName()
}
