package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.cdn.Client
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcontent.SteamContent
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.DepotManifest
import `in`.dragonbra.vapulla.util.SteamTimeoutException
import `in`.dragonbra.vapulla.util.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Fetches a depot's manifest, the list of files a build is made of, without downloading any
 * of the files. These are the same four steps the depot downloader takes before its first
 * chunk: the depot key, a manifest request code, a content server, and the manifest itself.
 */
class ManifestApi(
    client: SteamClient,
    private val steamApps: SteamApps,
    private val content: SteamContent,
    private val scope: CoroutineScope,
) {
    // One for the session. It downloads with the HTTP client of the SteamClient.
    private val cdn = Client(client)

    /**
     * @throws IllegalStateException with a message fit to show when Steam will not serve it,
     *   which is the case for depots the account has no license for.
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun manifest(
        appId: Int,
        depotId: Int,
        manifestId: Long,
        branch: String,
    ): DepotManifest {
        // File names in a manifest are encrypted with the depot's key.
        val key = steamApps.getDepotDecryptionKey(depotId, appId).awaitOrThrow()
        check(key.result == EResult.OK) { "Steam gave no key for depot $depotId: ${key.result}" }

        // Proof to the content servers that this account may have this manifest.
        val code = content.getManifestRequestCode(
            depotId = depotId,
            appId = appId,
            manifestId = manifestId,
            branch = branch,
            parentScope = scope,
        ).await()
        check(code != 0L) { "Steam gave no request code for manifest ${manifestId.toULong()}" }

        val servers = content.getServersForSteamPipe(parentScope = scope).await()
            .filter { it.type in SERVER_TYPES }
            .filter { it.allowedAppIds.isEmpty() || appId in it.allowedAppIds }
        check(servers.isNotEmpty()) { "Steam listed no content server for app $appId" }

        var lastError: Throwable? = null
        for (server in servers.take(MAX_SERVERS)) {
            // Not a plain try: leaving the screen cancels this call, and that must end the
            // loop rather than count as a server that did not serve.
            runSuspendCatching {
                cdn.downloadManifest(
                    depotId = depotId,
                    manifestId = manifestId,
                    manifestRequestCode = code.toULong(),
                    server = server,
                    depotKey = key.depotKey,
                )
            }.onSuccess {
                return it
            }.onFailure {
                Timber.w(it, "Manifest ${manifestId.toULong()} not served by ${server.host}")
                lastError = it
            }
        }
        error("No content server served the manifest: ${lastError?.message}")
    }

    private companion object {
        const val MAX_SERVERS = 3

        /** The kinds of content server that serve manifests to any client. */
        val SERVER_TYPES = setOf("SteamCache", "CDN")
    }
}
