package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesCloudSteamclient.CCloud_ClientFileDownload_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesCloudSteamclient.CCloud_EnumerateUserFiles_Request
import `in`.dragonbra.javasteam.rpc.service.Cloud
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Steam Cloud via the `Cloud` unified service: list a game's files and fetch one. */
class CloudApi(private val cloud: Cloud) {

    suspend fun files(appId: Int): List<CloudFile> {
        val request = CCloud_EnumerateUserFiles_Request.newBuilder()
            .setAppid(appId)
            .setExtendedDetails(true)
            .setCount(MAX_FILES)
            .build()

        val response = cloud.enumerateUserFiles(request).await()
        if (response.result != EResult.OK) {
            Timber.w("EnumerateUserFiles failed: ${response.result}")
            return emptyList()
        }

        return response.body.filesList.map { file ->
            CloudFile(
                appId = file.appid,
                filename = file.filename,
                size = file.fileSize.toLong(),
                timestamp = file.timestamp,
                sha = file.fileSha,
                platforms = file.platformsToSyncList.toImmutableList(),
            )
        }
    }

    /**
     * Downloads one file. Steam answers with a signed URL and headers, and the body may be
     * compressed, so it is inflated when the reported sizes differ.
     */
    suspend fun download(appId: Int, filename: String): ByteArray {
        val request = CCloud_ClientFileDownload_Request.newBuilder()
            .setAppid(appId)
            .setFilename(filename)
            .build()

        val response = cloud.clientFileDownload(request).await()
        check(response.result == EResult.OK) { "ClientFileDownload failed: ${response.result}" }

        val body = response.body
        check(!body.encrypted) { "Encrypted cloud files are not supported" }

        val scheme = if (body.useHttps) "https" else "http"
        val url = URL("$scheme://${body.urlHost}${body.urlPath}")

        val bytes = withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            try {
                body.requestHeadersList.forEach { connection.setRequestProperty(it.name, it.value) }
                check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                    "Cloud download returned HTTP ${connection.responseCode}"
                }
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        }

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
            .getOrElse { bytes }

    private companion object {
        const val MAX_FILES = 1000
    }
}
