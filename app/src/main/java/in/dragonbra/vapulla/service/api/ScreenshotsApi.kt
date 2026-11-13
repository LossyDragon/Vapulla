package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_GetUserFiles_Request
import `in`.dragonbra.javasteam.rpc.service.PublishedFile
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/** The account's uploaded screenshots via the `PublishedFile` unified service. View only. */
class ScreenshotsApi(
    private val publishedFile: PublishedFile,
    private val localSteamId: () -> SteamID?,
) {
    /** One page of the signed-in account's screenshots, newest first; pages start at 1. */
    suspend fun mine(page: Int = 1, perPage: Int = PAGE_SIZE): ScreenshotPage {
        val steamId = localSteamId() ?: return ScreenshotPage(persistentListOf(), 0, null)

        val request = CPublishedFile_GetUserFiles_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setType("myfiles")
            .setFiletype(FILE_TYPE_SCREENSHOTS)
            .setSortmethod("newestfirst")
            .setPrivacy(PRIVACY_ALL)
            .setPage(page)
            .setNumperpage(perPage)
            .setReturnApps(true)
            .build()

        val response = publishedFile.getUserFiles(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetUserFiles failed: ${response.result}")
            return ScreenshotPage(persistentListOf(), 0, null)
        }
        Timber.d(
            "GetUserFiles page=$page total=${response.body.total} " +
                "items=${response.body.publishedfiledetailsCount}",
        )

        val appNames = response.body.appsList.associate { it.appid to it.name }
        val items = response.body.publishedfiledetailsList.map { details ->
            Screenshot(
                id = details.publishedfileid,
                appId = details.consumerAppid,
                appName = appNames[details.consumerAppid].orEmpty(),
                caption = details.title,
                imageUrl = details.imageUrl.ifEmpty { details.fileUrl },
                thumbnailUrl = details.previewUrl.ifEmpty { details.imageUrl },
                width = details.imageWidth,
                height = details.imageHeight,
                timeCreated = details.timeCreated.toLong(),
            )
        }

        val seenSoFar = page * perPage
        return ScreenshotPage(
            items = items.toImmutableList(),
            total = response.body.total,
            nextPage = (page + 1).takeIf { items.isNotEmpty() && seenSoFar < response.body.total },
        )
    }

    private companion object {
        const val PAGE_SIZE = 30

        /** Steam's `EPublishedFileInfoMatchingFileType` value for screenshots. */
        const val FILE_TYPE_SCREENSHOTS = 4

        /** Visibility bits: private, friends only, public and unlisted. */
        const val PRIVACY_ALL = 30
    }
}
