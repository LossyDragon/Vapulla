package `in`.dragonbra.vapulla.service.handler

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientGetEmoticonList
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientEnableOrDisableDownloadsResponse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientGetClientAppListResponse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientInstallClientAppResponse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientSetClientAppUpdateStateResponse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientUninstallClientAppResponse
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.JobID
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.service.callback.RemoteAppListRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteDownloadsSwitchRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteInstallRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteUninstallRequestCallback
import `in`.dragonbra.vapulla.service.callback.RemoteUpdateStateRequestCallback

/**
 * App-specific JavaSteam handler for messages the library does not cover itself: the
 * emoticon list, and what another device of the account asks of this one: which games it
 * has, to download or remove one, and to pause or carry on with downloads.
 */
class VapullaHandler : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        val callback = getCallback(packetMsg) ?: return
        client.postCallback(callback)
    }

    /** Asks Steam for the account's emoticons; answered with an [EmoticonListCallback]. */
    fun getEmoticonList() {
        ClientMsgProtobuf<CMsgClientGetEmoticonList.Builder>(
            CMsgClientGetEmoticonList::class.java,
            EMsg.ClientGetEmoticonList,
        ).also(client::send)
    }

    /**
     * Answers a [RemoteAppListRequestCallback]. Steam matches the answer to the request by
     * [requestJobId], and hands it on to the device that asked.
     */
    fun sendClientAppList(requestJobId: JobID, apps: CMsgClientGetClientAppListResponse.Builder) {
        val response = ClientMsgProtobuf<CMsgClientGetClientAppListResponse.Builder>(
            CMsgClientGetClientAppListResponse::class.java,
            EMsg.ClientGetClientAppListResponse,
        )
        response.targetJobID = requestJobId
        response.body.mergeFrom(apps.build())

        client.send(response)
    }

    /** Answers a [RemoteInstallRequestCallback]: [result] is `OK` when the game was queued. */
    fun sendInstallResult(requestJobId: JobID, result: EResult) {
        val response = ClientMsgProtobuf<CMsgClientInstallClientAppResponse.Builder>(
            CMsgClientInstallClientAppResponse::class.java,
            EMsg.ClientInstallClientAppResponse,
        )
        response.targetJobID = requestJobId
        response.body.result = result.code()

        client.send(response)
    }

    /** Answers a [RemoteUpdateStateRequestCallback]. */
    fun sendUpdateStateResult(requestJobId: JobID, result: EResult) {
        val response = ClientMsgProtobuf<CMsgClientSetClientAppUpdateStateResponse.Builder>(
            CMsgClientSetClientAppUpdateStateResponse::class.java,
            EMsg.ClientSetClientAppUpdateStateResponse,
        )
        response.targetJobID = requestJobId
        response.body.result = result.code()

        client.send(response)
    }

    /** Answers a [RemoteDownloadsSwitchRequestCallback]. */
    fun sendDownloadsSwitchResult(requestJobId: JobID, result: EResult) {
        val response = ClientMsgProtobuf<CMsgClientEnableOrDisableDownloadsResponse.Builder>(
            CMsgClientEnableOrDisableDownloadsResponse::class.java,
            EMsg.ClientEnableOrDisableDownloadsResponse,
        )
        response.targetJobID = requestJobId
        response.body.result = result.code()

        client.send(response)
    }

    /** Answers a [RemoteUninstallRequestCallback]: [result] is `OK` when the game is being removed. */
    fun sendUninstallResult(requestJobId: JobID, result: EResult) {
        val response = ClientMsgProtobuf<CMsgClientUninstallClientAppResponse.Builder>(
            CMsgClientUninstallClientAppResponse::class.java,
            EMsg.ClientUninstallClientAppResponse,
        )
        response.targetJobID = requestJobId
        response.body.result = result.code()

        client.send(response)
    }

    private companion object {
        fun getCallback(packetMsg: IPacketMsg): CallbackMsg? = when (packetMsg.msgType) {
            EMsg.ClientEmoticonList -> EmoticonListCallback(packetMsg)

            EMsg.ClientGetClientAppList -> RemoteAppListRequestCallback(packetMsg)

            EMsg.ClientInstallClientApp -> RemoteInstallRequestCallback(packetMsg)

            EMsg.ClientUninstallClientApp -> RemoteUninstallRequestCallback(packetMsg)

            EMsg.ClientSetClientAppUpdateState -> RemoteUpdateStateRequestCallback(packetMsg)

            EMsg.ClientEnableOrDisableDownloads ->
                RemoteDownloadsSwitchRequestCallback(packetMsg)

            else -> null
        }
    }
}
