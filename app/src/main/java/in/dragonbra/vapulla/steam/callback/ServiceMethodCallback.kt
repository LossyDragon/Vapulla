package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_NotificationOrBuilder
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.SteamID

class ServiceMethodCallback(
        msg: CFriendMessages_IncomingMessage_NotificationOrBuilder,
        targetJobName: String
) : CallbackMsg() {
    val steamID: SteamID = SteamID(msg.steamidFriend)
    val message: String = msg.message
    val entryType: EChatEntryType = EChatEntryType.from(msg.chatEntryType)
    val jobName: String = targetJobName // Header -> Proto -> getTargetJobName()
}
