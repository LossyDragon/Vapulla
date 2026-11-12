package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.ERegionCode
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.QueryDetails
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.vapulla.util.SourceQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await

/**
 * Game server browser: the `SteamMasterServer` handler lists servers for an app, then each
 * one is asked directly for its details with [SourceQuery].
 */
class ServerBrowserApi(private val masterServer: SteamMasterServer) {

    /**
     * Lists up to [maxServers] servers for [appId] and waits [timeoutMs] for each A2S reply.
     * [filter] uses the master server filter syntax, e.g. `\empty\1`; it must not be null
     * because JavaSteam passes it straight to the protobuf builder.
     */
    suspend fun query(
        appId: Int,
        region: ERegionCode = ERegionCode.World,
        filter: String = "",
        maxServers: Int = 50,
        timeoutMs: Int = 1500,
    ): List<GameServer> {
        val details = QueryDetails(
            appID = appId,
            filter = filter,
            region = region,
            geoLocatedIP = null,
            maxServers = maxServers,
        )

        val servers = masterServer.serverQuery(details).await().servers

        return coroutineScope {
            servers.map { server ->
                async {
                    GameServer(
                        host = server.endPoint.address.hostAddress.orEmpty(),
                        port = server.endPoint.port,
                        authedPlayers = server.authedPlayers,
                        info = SourceQuery.info(server.endPoint, timeoutMs),
                    )
                }
            }.awaitAll()
        }
    }
}
