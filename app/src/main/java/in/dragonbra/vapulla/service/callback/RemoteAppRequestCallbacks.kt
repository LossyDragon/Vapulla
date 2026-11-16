package `in`.dragonbra.vapulla.service.callback

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientEnableOrDisableDownloads
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientInstallClientApp
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientSetClientAppUpdateState
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverUds.CMsgClientUninstallClientApp
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.JobID

/**
 * Another device of the account asks this client to download a game, with
 * `ClientInstallClientApp`: someone picked this device there and pressed Install. Steam
 * waits for a `ClientInstallClientAppResponse`.
 *
 * @property requestJobId names the request; the answer must carry it as its target job.
 */
class RemoteInstallRequestCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val requestJobId: JobID
    val appId: Int

    init {
        val request = ClientMsgProtobuf<CMsgClientInstallClientApp.Builder>(
            CMsgClientInstallClientApp::class.java,
            packetMsg,
        )

        requestJobId = request.sourceJobID
        appId = request.body.appid
    }
}

/**
 * Another device of the account asks this client to pause a game's download or to carry on
 * with it, with `ClientSetClientAppUpdateState`. Steam waits for a
 * `ClientSetClientAppUpdateStateResponse`.
 *
 * @property update true to carry on with the download, false to pause it.
 */
class RemoteUpdateStateRequestCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val requestJobId: JobID
    val appId: Int
    val update: Boolean

    init {
        val request = ClientMsgProtobuf<CMsgClientSetClientAppUpdateState.Builder>(
            CMsgClientSetClientAppUpdateState::class.java,
            packetMsg,
        )

        requestJobId = request.sourceJobID
        appId = request.body.appid
        update = request.body.update
    }
}

/**
 * Another device of the account asks this client to stop downloading altogether, or to carry
 * on, with `ClientEnableOrDisableDownloads`: what the pause button of Steam's own client
 * sends for a download on another device. Steam waits for a
 * `ClientEnableOrDisableDownloadsResponse`.
 *
 * @property enable true to carry on with the downloads, false to stop them.
 */
class RemoteDownloadsSwitchRequestCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val requestJobId: JobID
    val enable: Boolean

    init {
        val request = ClientMsgProtobuf<CMsgClientEnableOrDisableDownloads.Builder>(
            CMsgClientEnableOrDisableDownloads::class.java,
            packetMsg,
        )

        requestJobId = request.sourceJobID
        enable = request.body.enable
    }
}

/**
 * Another device of the account asks this client to remove a game, with
 * `ClientUninstallClientApp`. Steam waits for a `ClientUninstallClientAppResponse`.
 */
class RemoteUninstallRequestCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val requestJobId: JobID
    val appId: Int

    init {
        val request = ClientMsgProtobuf<CMsgClientUninstallClientApp.Builder>(
            CMsgClientUninstallClientApp::class.java,
            packetMsg,
        )

        requestJobId = request.sourceJobID
        appId = request.body.appid
    }
}
