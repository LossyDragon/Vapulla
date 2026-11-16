package `in`.dragonbra.vapulla.service.callback

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientGetClientAppList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.JobID

/**
 * Another device of the account asks which games this client has, with `ClientGetClientAppList`.
 * Steam sends it when someone calls the `ClientComm` service for this client, as the Remote
 * Clients screen does for others, and waits for a `ClientGetClientAppListResponse`.
 *
 * @property requestJobId names the request; the answer must carry it as its target job.
 */
class RemoteAppListRequestCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val requestJobId: JobID

    /** Only the games that are being downloaded or updated are wanted. */
    val onlyChanging: Boolean

    val onlyInstalled: Boolean

    /** The answer should say what machine this is and how much room it has. */
    val includeClientInfo: Boolean

    /** When not empty, only these apps are asked about. */
    val appIds: List<Int>

    init {
        val request = ClientMsgProtobuf<CMsgClientGetClientAppList.Builder>(
            CMsgClientGetClientAppList::class.java,
            packetMsg,
        )

        requestJobId = request.sourceJobID
        onlyChanging = request.body.onlyChanging
        onlyInstalled = request.body.onlyInstalled
        includeClientInfo = request.body.includeClientInfo
        appIds = request.body.filterAppidsList
    }
}
