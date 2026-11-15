package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesCloudSteamclient.CCloud_ClientFileDownload_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesCloudSteamclient.CCloud_EnumerateUserFiles_Request
import `in`.dragonbra.javasteam.rpc.service.Cloud
import `in`.dragonbra.vapulla.util.httpGet
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream
import kotlinx.collections.immutable.toImmutableList

/** Steam Cloud via the `Cloud` unified service: list a game's files and fetch one. */
class CloudApi(private val cloud: Cloud) {

    /**
     * The account's cloud files of [appId], at most [MAX_FILES] of them. Empty when Steam does
     * not answer.
     */
    suspend fun files(appId: Int): List<CloudFile> {
        val request = CCloud_EnumerateUserFiles_Request.newBuilder()
            .setAppid(appId)
            .setExtendedDetails(true)
            .setCount(MAX_FILES)
            .build()

        val body = cloud.enumerateUserFiles(request).bodyOrNull("EnumerateUserFiles")
            ?: return emptyList()

        return body.filesList.map { file ->
            CloudFile(
                appId = file.appid,
                filename = file.filename,
                size = file.fileSize.toLong(),
                timestamp = file.timestamp,
                platforms = file.platformsToSyncList.toImmutableList(),
            )
        }
    }

    /**
     * Downloads one file. Steam answers with a signed URL and headers, and the body may be
     * compressed, so it is inflated when the reported sizes differ.
     *
     * @throws IllegalStateException if Steam refuses, or the file is encrypted.
     * @throws IOException if the download fails or what arrives cannot be unpacked.
     */
    suspend fun download(appId: Int, filename: String): ByteArray {
        val request = CCloud_ClientFileDownload_Request.newBuilder()
            .setAppid(appId)
            .setFilename(filename)
            .build()

        val body = cloud.clientFileDownload(request).bodyOrThrow("ClientFileDownload")
        check(!body.encrypted) { "Encrypted cloud files are not supported" }

        val scheme = if (body.useHttps) "https" else "http"
        val bytes = httpGet(
            url = "$scheme://${body.urlHost}${body.urlPath}",
            headers = body.requestHeadersList.associate { it.name to it.value },
        )

        return if (body.fileSize != body.rawFileSize) decompress(bytes) else bytes
    }

    /** Cloud files are stored zlib-deflated, older ones as a single-entry zip. */
    private fun decompress(bytes: ByteArray): ByteArray =
        runCatching { InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() } }
            .recoverCatching {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    checkNotNull(zip.nextEntry) { "Empty zip" }
                    zip.readBytes()
                }
            }
            .getOrElse { throw IOException("The cloud file is compressed in an unknown way", it) }

    private companion object {
        const val MAX_FILES = 1000
    }
}
