package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.protobufs.webui.ServiceClientcomm.CClientComm_GetAllClientLogonInfo_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceClientcomm.CClientComm_GetClientAppList_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceClientcomm.CClientComm_GetClientAppList_Response
import `in`.dragonbra.javasteam.rpc.service.webui.ClientComm
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The Steam clients signed in to the account on other machines, and the games on them, via
 * the `ClientComm` service: what Steam's own mobile app shows as remote downloads. The
 * service comes from JavaSteam's `javasteam-protobufs-webui` artifact.
 *
 * Only reading is done here. The service can also install, pause, uninstall and launch a
 * game on another machine.
 */
class RemoteClientsApi(private val clientComm: ClientComm, private val ownInstanceId: () -> Long) {

    /**
     * The other clients that answer for themselves. Steam lists every session of the account,
     * but not every one is a client that can say what is on its machine: an older Vapulla,
     * or a tool signed in with the account, never answers. So each is asked, all at once,
     * and those that stay silent for [ANSWER_TIMEOUT] are left out.
     *
     * @return empty when Steam does not answer.
     */
    suspend fun clients(): List<RemoteClient> {
        val request = CClientComm_GetAllClientLogonInfo_Request.newBuilder().build()
        val sessions = clientComm.getAllClientLogonInfo(request)
            .bodyOrNull("GetAllClientLogonInfo")
            ?.sessionsList
            .orEmpty()

        return coroutineScope {
            sessions
                // This device is in the list as well, and has its Downloads screen for that.
                .filterNot { it.clientInstanceid == ownInstanceId() }
                .map { session ->
                    async {
                        // Asking for the games that are changing keeps the answer short;
                        // the client's own details come with it.
                        val answer = withTimeoutOrNull(ANSWER_TIMEOUT) {
                            appList(session.clientInstanceid, FILTER_CHANGING)
                        }
                            ?: return@async null

                        RemoteClient(
                            id = session.clientInstanceid,
                            machineName = session.machineName,
                            osName = session.osName,
                            freeBytes = answer.bytesAvailable,
                            runningAppIds = answer.clientInfo.runningGamesList
                                .map { it.appid }
                                .toImmutableList(),
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    /**
     * The games of the account as the client [clientId] has them, by name.
     *
     * @return null when the client does not answer, which it stops doing once it is closed.
     */
    suspend fun games(clientId: Long): List<RemoteApp>? = appList(clientId, FILTER_NONE)
        ?.appsList
        ?.map { app ->
            RemoteApp(
                appId = app.appid,
                name = app.app.ifBlank { "App ${app.appid}" },
                installed = app.installed,
                running = app.running,
                changing = app.changing,
                paused = app.downloadPaused,
                bytesDownloaded = app.bytesDownloaded,
                bytesToDownload = app.bytesToDownload,
                downloadRate = app.bytesDownloadRate.toUInt().toLong(),
                secondsRemaining = app.estimatedSecondsRemaining,
                queuePosition = app.queuePosition,
                bytesRequired = app.bytesRequired,
            )
        }
        ?.sortedBy { it.name.lowercase() }

    private suspend fun appList(
        clientId: Long,
        filter: String,
    ): CClientComm_GetClientAppList_Response.Builder? {
        val request = CClientComm_GetClientAppList_Request.newBuilder()
            .setClientInstanceid(clientId)
            .setFields(FIELDS_GAMES)
            .setFilters(filter)
            .setIncludeClientInfo(true)
            .setLanguage(STEAM_LANGUAGE)
            .build()

        return clientComm.getClientAppList(request).bodyOrNull("GetClientAppList")
    }

    private companion object {
        /** Games only; "all" adds tools and the like. */
        const val FIELDS_GAMES = "games"
        const val FILTER_NONE = "none"

        /** A real client answers within a second or two; Steam itself waits ten. */
        val ANSWER_TIMEOUT = 4.seconds

        /** Only the apps that are being downloaded, updated or removed. */
        const val FILTER_CHANGING = "changing"
    }
}
