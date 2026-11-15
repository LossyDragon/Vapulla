package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_GetUserFiles_Request
import `in`.dragonbra.javasteam.rpc.service.PublishedFile
import `in`.dragonbra.javasteam.types.SteamID
import timber.log.Timber

/** The account's uploaded screenshots via the `PublishedFile` unified service. View only. */
class ScreenshotsApi(
    private val publishedFile: PublishedFile,
    private val localSteamId: () -> SteamID?,
) {
    /**
     * One page of the signed-in account's screenshots, newest first; pages start at 1. Empty
     * when Steam does not answer.
     */
    suspend fun mine(page: Int = 1): Page<Screenshot> {
        val steamId = localSteamId() ?: return Page.empty()

        val request = CPublishedFile_GetUserFiles_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setType("myfiles")
            .setFiletype(FILE_TYPE_SCREENSHOTS)
            .setSortmethod("newestfirst")
            .setPrivacy(PRIVACY_ALL)
            .setPage(page)
            .setNumperpage(PAGE_SIZE)
            .setReturnApps(true)
            .build()

        val body = publishedFile.getUserFiles(request).bodyOrNull("GetUserFiles")
            ?: return Page.empty()
        Timber.d(
            "GetUserFiles page=$page total=${body.total} items=${body.publishedfiledetailsCount}",
        )

        val appNames = body.appsList.associate { it.appid to it.name }
        val items = body.publishedfiledetailsList.map { details ->
            Screenshot(
                id = details.publishedfileid,
                appId = details.consumerAppid,
                appName = appNames[details.consumerAppid].orEmpty(),
                caption = details.title,
                imageUrl = details.imageUrl.ifEmpty { details.fileUrl },
                thumbnailUrl = details.previewUrl.ifEmpty { details.imageUrl },
                width = details.imageWidth,
                height = details.imageHeight,
                timeCreated = details.timeCreated.toUInt().toLong(),
            )
        }

        return Page.of(items, page, PAGE_SIZE, body.total)
    }

    private companion object {
        const val PAGE_SIZE = 30

        /** Steam's `EPublishedFileInfoMatchingFileType` value for screenshots. */
        const val FILE_TYPE_SCREENSHOTS = 4

        /** Visibility bits: private, friends only, public and unlisted. */
        const val PRIVACY_ALL = 30
    }
}
